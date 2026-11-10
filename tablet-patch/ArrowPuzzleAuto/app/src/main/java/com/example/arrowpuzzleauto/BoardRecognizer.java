package com.example.arrowpuzzleauto;

import java.util.Arrays;
import java.util.List;

public final class BoardRecognizer {
    public static final class Result {
        public final int[] states;
        public final int unknownTiles;
        public final boolean claimTextVisible;
        public final int geometryRotation;
        final int confidence;
        public Result(int[] states,int unknownTiles,boolean claimTextVisible,
                      int geometryRotation,int confidence) {
            this.states=states; this.unknownTiles=unknownTiles;
            this.claimTextVisible=claimTextVisible; this.geometryRotation=geometryRotation;
            this.confidence=confidence;
        }
        public boolean isSolved() {
            if(unknownTiles!=0)return false;
            for(int state:states)if(state!=1)return false;
            return true;
        }
    }
    private static final int[] ANGLES={0,45,90,135,180,225,270,315};

    public Result read(ScreenFrame frame) {
        if(frame==null || frame.width<300 || frame.height<600)return null;
        float aspect=(float)frame.width/frame.height;
        if(!ArrowPuzzleGeometry.isSupportedAspect(aspect))return null;
        return readWithRotation(frame,0);
    }

    private Result readWithRotation(ScreenFrame frame,int rotation) {
        List<ArrowPuzzleGeometry.PointN> centers=
                ArrowPuzzleGeometry.centers(frame.width,frame.height,rotation);
        int radius=ArrowPuzzleGeometry.sampleRadiusPx(frame.width,frame.height);
        int[] states=new int[centers.size()];
        int unknown=0,confidence=0;
        for(int i=0;i<centers.size();i++) {
            ArrowPuzzleGeometry.PointN p=centers.get(i);
            int cx=Math.round(p.x*frame.width), cy=Math.round(p.y*frame.height);
            int[] samples=new int[ANGLES.length];
            for(int j=0;j<ANGLES.length;j++) {
                double a=Math.toRadians(ANGLES[j]);
                samples[j]=frame.luminance(
                    cx+Math.round((float)(radius*Math.cos(a))),
                    cy+Math.round((float)(radius*Math.sin(a))));
            }
            Arrays.sort(samples);
            int median=(samples[3]+samples[4])/2;
            if(median<48){states[i]=1;confidence+=48-median;}
            else if(median>65){states[i]=2;confidence+=median-65;}
            else{states[i]=0;unknown++;confidence-=100;}
        }
        return new Result(states,unknown,isClaimTextVisible(frame),rotation,confidence);
    }

    private boolean isClaimTextVisible(ScreenFrame frame) {
        boolean tablet=ArrowPuzzleGeometry.isTabletProfile((float)frame.width/frame.height);
        if(tablet && frame.width>=frame.height) {
            // The completion banner is centered above the Android dock.
            // The narrower vertical band excludes bright dock icons below it.
            int left=Math.round(frame.width*0.38f);
            int right=Math.round(frame.width*0.62f);
            int top=Math.round(frame.height*0.925f);
            int bottom=Math.round(frame.height*0.947f);
            int count=0,minX=right,maxX=-1,minY=bottom,maxY=-1;
            for(int y=top;y<bottom;y++) {
                for(int x=left;x<right;x++) {
                    if(frame.luminance(x,y)>=185) {
                        count++;
                        minX=Math.min(minX,x); maxX=Math.max(maxX,x);
                        minY=Math.min(minY,y); maxY=Math.max(maxY,y);
                    }
                }
            }
            if(maxX<minX)return false;
            int spanX=maxX-minX+1, spanY=maxY-minY+1;
            return count>=350 && spanX>=frame.width*0.10f
                    && spanY>=frame.height*0.007f && spanY<=frame.height*0.035f;
        }

        // Portrait/phone fallback.
        float y0=tablet?0.895f:0.86f;
        float y1=tablet?0.925f:0.90f;
        int left=Math.max(0,Math.round(frame.width*0.20f));
        int right=Math.min(frame.width,Math.round(frame.width*0.75f));
        int top=Math.max(0,Math.round(frame.height*y0));
        int bottom=Math.min(frame.height,Math.round(frame.height*y1));
        int minX=right,maxX=-1;
        for(int y=top;y<bottom;y++)for(int x=left;x<right;x++) {
            if(frame.luminance(x,y)>=185) {
                minX=Math.min(minX,x); maxX=Math.max(maxX,x);
            }
        }
        return maxX>=minX && maxX-minX+1>=frame.width*0.10f;
    }
}
