package com.example.arrowpuzzleauto;

import java.util.Arrays;
import java.util.List;

public final class BoardRecognizer {
    public static final class Result {
        public final int[] states;
        public final int unknownTiles;
        public final boolean claimTextVisible;
        public Result(int[] states,int unknownTiles,boolean claimTextVisible) {
            this.states=states; this.unknownTiles=unknownTiles; this.claimTextVisible=claimTextVisible;
        }
        public boolean isSolved() {
            if (unknownTiles!=0) return false;
            for (int state:states) if (state!=1) return false;
            return true;
        }
    }
    private static final int[] ANGLES={0,45,90,135,180,225,270,315};
    private static final float CLAIM_LABEL_MIN_SPAN=0.33f;

    public Result read(ScreenFrame frame) {
        if(frame==null || frame.width<300 || frame.height<600) return null;
        float aspect=(float)frame.width/frame.height;
        if(aspect<0.43f || aspect>0.70f) return null;
        List<ArrowPuzzleGeometry.PointN> centers=ArrowPuzzleGeometry.centers(aspect);
        int[] states=new int[centers.size()];
        int unknown=0;
        int radius=Math.max(8,Math.round(ArrowPuzzleGeometry.sampleRadiusN(aspect)*frame.width));
        for(int i=0;i<centers.size();i++) {
            ArrowPuzzleGeometry.PointN p=centers.get(i);
            int cx=Math.round(p.x*frame.width), cy=Math.round(p.y*frame.height);
            int[] samples=new int[ANGLES.length];
            for(int j=0;j<ANGLES.length;j++) {
                double a=Math.toRadians(ANGLES[j]);
                samples[j]=frame.luminance(cx+Math.round((float)(radius*Math.cos(a))),cy+Math.round((float)(radius*Math.sin(a))));
            }
            Arrays.sort(samples);
            int median=(samples[3]+samples[4])/2;
            if(median<48) states[i]=1;
            else if(median>65) states[i]=2;
            else {states[i]=0;unknown++;}
        }
        return new Result(states,unknown,isClaimTextVisible(frame,aspect));
    }

    private boolean isClaimTextVisible(ScreenFrame frame,float aspect) {
        boolean tablet=ArrowPuzzleGeometry.isTabletProfile(aspect);
        int x0=Math.round(frame.width*(tablet?0.20f:0.20f));
        int x1=Math.round(frame.width*(tablet?0.75f:0.75f));
        int y0=Math.round(frame.height*(tablet?0.895f:0.86f));
        int y1=Math.round(frame.height*(tablet?0.925f:0.90f));
        int minX=x1,maxX=-1;
        for(int y=y0;y<y1;y++) for(int x=x0;x<x1;x++) if(frame.luminance(x,y)>=185) {
            if(x<minX) minX=x; if(x>maxX) maxX=x;
        }
        return maxX>=minX && (maxX-minX+1)>=frame.width*CLAIM_LABEL_MIN_SPAN;
    }
}