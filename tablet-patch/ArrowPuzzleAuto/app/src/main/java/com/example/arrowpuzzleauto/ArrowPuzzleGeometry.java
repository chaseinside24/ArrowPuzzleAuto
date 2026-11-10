package com.example.arrowpuzzleauto;

import java.util.ArrayList;
import java.util.List;

public final class ArrowPuzzleGeometry {
    private ArrowPuzzleGeometry() {}

    public static final class PointN {
        public final float x, y;
        public PointN(float x, float y) { this.x = x; this.y = y; }
    }

    private static final float[] PHONE_X = {93f/709f,180f/709f,267f/709f,354f/709f,442f/709f,529f/709f,616f/709f};
    private static final int[][] PHONE_Y = {
        {723,824,925,1025},{673,773,874,975,1075},{622,723,824,925,1025,1125},
        {572,673,773,874,975,1075,1176},{622,723,824,925,1025,1125},{673,773,874,975,1075},{723,824,925,1025}
    };
    // Galaxy Tab S10 FE profile measured from the supplied 960x1536 screenshot.
    private static final float[] TABLET_X = {153f/960f,261f/960f,371f/960f,481f/960f,591f/960f,699f/960f,809f/960f};
    private static final int[][] TABLET_Y = {
        {641,767,893,1020},{578,704,829,958,1084},{514,641,767,893,1021,1148},
        {452,578,706,830,957,1083,1211},{514,641,768,893,1021,1147},{579,705,831,957,1083},{640,768,895,1021}
    };

    public static boolean isTabletProfile(float aspect) { return aspect >= 0.55f; }

    public static List<PointN> centers(float aspect) {
        boolean tablet = isTabletProfile(aspect);
        float[] xs = tablet ? TABLET_X : PHONE_X;
        int[][] ys = tablet ? TABLET_Y : PHONE_Y;
        float baseHeight = 1536f;
        List<PointN> result = new ArrayList<>(37);
        for (int c=0;c<xs.length;c++) for (int y:ys[c]) result.add(new PointN(xs[c], y/baseHeight));
        return result;
    }

    public static float sampleRadiusN(float aspect) {
        return isTabletProfile(aspect) ? 32f/960f : 24f/709f;
    }

    public static PointN claimButtonCenter(float aspect) {
        return isTabletProfile(aspect)
            ? new PointN(443f/960f,1393f/1536f)
            : new PointN(313f/709f,1352f/1536f);
    }
}