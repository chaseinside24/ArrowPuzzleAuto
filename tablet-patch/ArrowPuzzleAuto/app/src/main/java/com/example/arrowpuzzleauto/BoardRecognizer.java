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
    private static final float CLAIM_LABEL_MIN_SPAN=0.33f;

    public Result read(ScreenFrame frame) {
        if(frame==null || frame.width<300 || frame.height<600)return null;
        float aspect=(float)frame.width/frame.height;
        if(!ArrowPuzzleGeometry.isSupportedAspect(aspect))return null;

        // The landscape tablet coordinates are calibrated directly in landscape pixels.
        int[] rotations=ArrowPuzzleGeometry.isTabletProfile(aspect) && frame.width>=frame.height
                ? new int[]{0} : new int[]{0};
        Result best=null;
        for(int rotation:rotations) {
            Result candidate=readWithRotation(frame,rotation);
            if(best==null || candidate.unknownTiles<best.unknownTiles
                    || (candidate.unknownTiles==best.unknownTiles
                        && candidate.confidence>best.confidence)) best=candidate;
        }
        return best;
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
        return new Result(states,unknown,isClaimTextVisible(frame,rotation),rotation,confidence);
    }
    private boolean isClaimTextVisible(ScreenFrame frame,int rotation) {
        boolean tablet=ArrowPuzzleGeometry.isTabletProfile((float)frame.width/frame.height);
        if(tablet && frame.width>=frame.height) {
            // Completion text/button is in the lower central game area in landscape.
            return hasBrightSpan(frame,0.30f,0.70f,0.87f,0.97f,true);
        }
        float y0=tablet?0.895f:0.86f, y1=tablet?0.925f:0.90f;
        return hasBrightSpan(frame,0.20f,0.75f,y0,y1,false);
    }
    private boolean hasBrightSpan(ScreenFrame frame,float x0,float x1,float y0,float y1,
                                  boolean vertical) {
        int left=Math.max(0,Math.round(x0*frame.width));
        int right=Math.min(frame.width,Math.round(x1*frame.width));
        int top=Math.max(0,Math.round(y0*frame.height));
        int bottom=Math.min(frame.height,Math.round(y1*frame.height));
        int minX=right,maxX=-1,minY=bottom,maxY=-1;
        for(int y=top;y<bottom;y++)for(int x=left;x<right;x++) {
            if(frame.luminance(x,y)>=185) {
                minX=Math.min(minX,x);maxX=Math.max(maxX,x);
                minY=Math.min(minY,y);maxY=Math.max(maxY,y);
            }
        }
        if(maxX<minX)return false;
        int span=vertical?maxY-minY+1:maxX-minX+1;
        return span>=Math.min(frame.width,frame.height)*CLAIM_LABEL_MIN_SPAN;
    }
}
