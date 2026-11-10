package com.example.arrowpuzzleauto;

import java.util.ArrayList;
import java.util.List;

public final class ArrowPuzzleGeometry {
    private ArrowPuzzleGeometry() {}
    public static final class PointN {
        public final float x, y;
        public PointN(float x, float y) { this.x=x; this.y=y; }
    }

    private static final float[] PHONE_X = {
        93f/709f,180f/709f,267f/709f,354f/709f,442f/709f,529f/709f,616f/709f
    };
    private static final int[][] PHONE_Y = {
        {723,824,925,1025},{673,773,874,975,1075},{622,723,824,925,1025,1125},
        {572,673,773,874,975,1075,1176},{622,723,824,925,1025,1125},
        {673,773,874,975,1075},{723,824,925,1025}
    };

    // Galaxy Tab S10 FE landscape screenshot: 1536 x 960.
    private static final float[] TABLET_LANDSCAPE_X = {
        551f/1536f,624f/1536f,696f/1536f,768f/1536f,
        841f/1536f,914f/1536f,986f/1536f
    };
    private static final int[][] TABLET_LANDSCAPE_Y = {
        {416,500,584,668},
        {374,458,542,626,710},
        {332,416,500,584,668,752},
        {290,374,458,542,626,710,794},
        {332,416,500,584,668,752},
        {374,458,542,626,710},
        {416,500,584,668}
    };

    // Portrait fallback profile.
    private static final float[] TABLET_PORTRAIT_X = {
        153f/960f,261f/960f,371f/960f,481f/960f,591f/960f,699f/960f,809f/960f
    };
    private static final int[][] TABLET_PORTRAIT_Y = {
        {641,767,893,1020},{578,704,829,958,1084},{514,641,767,893,1021,1148},
        {452,578,706,830,957,1083,1211},{514,641,768,893,1021,1147},
        {579,705,831,957,1083},{640,768,895,1021}
    };

    public static boolean isSupportedAspect(float aspect) {
        if (aspect <= 0f) return false;
        float shortOverLong=Math.min(aspect,1f/aspect);
        return shortOverLong>=0.40f && shortOverLong<=0.70f;
    }
    public static boolean isTabletProfile(float aspect) {
        return aspect>0f && Math.min(aspect,1f/aspect)>=0.55f;
    }
    public static boolean isLandscape(int width,int height) { return width>=height; }

    public static List<PointN> centers(int width,int height,int rotation) {
        float aspect=(float)width/height;
        boolean tablet=isTabletProfile(aspect);
        float[] xs;
        int[][] ys;
        float yDenom;
        if (tablet && width>=height) {
            xs=TABLET_LANDSCAPE_X; ys=TABLET_LANDSCAPE_Y; yDenom=960f;
        } else if (tablet) {
            xs=TABLET_PORTRAIT_X; ys=TABLET_PORTRAIT_Y; yDenom=1536f;
        } else {
            xs=PHONE_X; ys=PHONE_Y; yDenom=1536f;
        }
        List<PointN> points=new ArrayList<>(37);
        for(int c=0;c<xs.length;c++)
            for(int y:ys[c]) points.add(new PointN(xs[c],y/yDenom));
        return points;
    }
    public static int sampleRadiusPx(int width,int height) {
        float aspect=(float)width/height;
        float fraction=isTabletProfile(aspect)?32f/960f:24f/709f;
        return Math.max(8,Math.round(fraction*Math.min(width,height)));
    }
    public static PointN claimButtonCenter(int width,int height,int rotation) {
        if(isTabletProfile((float)width/height) && width>=height)
            return new PointN(0.5f,0.94f);
        if(isTabletProfile((float)width/height))
            return new PointN(443f/960f,1393f/1536f);
        return new PointN(313f/709f,1352f/1536f);
    }
    public static PointN orient(PointN p,int rotation) {
        switch(rotation) {
            case 1:return new PointN(1f-p.y,p.x);
            case 2:return new PointN(1f-p.x,1f-p.y);
            case 3:return new PointN(p.y,1f-p.x);
            default:return p;
        }
    }
}
