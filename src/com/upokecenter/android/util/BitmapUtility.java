package com.upokecenter.android.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.Surface;

public final class BitmapUtility {
	private BitmapUtility(){}
	private static Matrix parallelogramMatrix(
			RectF srcRect, // Position of a rectangle
			float[] destPara// how the top left/top right/bottom left
			// corners of the rectangle
			// are now expressed
			){
		if(destPara==null||srcRect==null||destPara.length<6)
			throw new IllegalArgumentException();
		float srcPara[]=new float[]{
				srcRect.left, // top left corner
				srcRect.top,
				srcRect.right, // top right corner
				srcRect.top,
				srcRect.left, // bottom left corner
				srcRect.bottom
		};
		float matSrc[]=new float[]{
				srcPara[0]-srcPara[4],srcPara[2]-srcPara[4],srcPara[4],
				srcPara[1]-srcPara[5],srcPara[3]-srcPara[5],srcPara[5],
				0,0,1
		};
		float matDst[]=new float[]{
				destPara[0]-destPara[4],destPara[2]-destPara[4],destPara[4],
				destPara[1]-destPara[5],destPara[3]-destPara[5],destPara[5],
				0,0,1
		};
		Matrix matrixSrc=new Matrix();
		Matrix matrixDst=new Matrix();
		Matrix matrixInv=new Matrix();
		matrixSrc.setValues(matSrc);
		matrixDst.setValues(matDst);
		matrixSrc.invert(matrixInv);
		matrixDst.preConcat(matrixInv);
		return matrixDst;
	}

	private static Object syncRoot=new Object();
	private static int oldRotation=0;

	private static int getOldRotation(){
		synchronized(syncRoot){ return oldRotation; }
	}
	private static void setOldRotation(int val){
		synchronized(syncRoot){ oldRotation=val; }
	}

	public static Bitmap redrawBitmap(
			Bitmap oldBitmap,
			int width,
			int height,
			int background
			){
		Bitmap.Config oldConfig = null;
		int oldWidth=0;
		int oldHeight=0;
		boolean hasOldBitmap=(oldBitmap!=null);
		if(oldBitmap!=null){
			oldConfig=oldBitmap.getConfig();
			oldWidth=oldBitmap.getWidth();
			oldHeight=oldBitmap.getHeight();
			if(oldWidth==width && oldHeight==height){
				if(getOldRotation()==AppManager.getRotation())
					// No need to change the bitmap unless the rotation
					// has changed
					//DebugUtility.log("rotation unchanged");
					return oldBitmap;
			}
		} else {
			oldConfig=Bitmap.Config.ARGB_8888;
		}
		if(width<=0) {
			width=1;
		}
		if(height<=0) {
			height=1;
		}
		Bitmap newBitmap=Bitmap.createBitmap(width,height,oldConfig);
		Canvas c=new Canvas(newBitmap);
		if(Color.alpha(background)>0){
			Paint p=new Paint();
			p.setColor(background);
			c.drawRect(new Rect(0,0,width,height),p);
		}
		if(hasOldBitmap){
			Matrix mat=null;
			int rotation=AppManager.getRotation();
			int oldRotation=getOldRotation();
			setOldRotation(rotation);
			boolean turnedClockwise=true;
			if((oldRotation==Surface.ROTATION_270 && rotation==Surface.ROTATION_0) ||
					(oldRotation==Surface.ROTATION_0 && rotation==Surface.ROTATION_90) ||
					(oldRotation==Surface.ROTATION_90 && rotation==Surface.ROTATION_180) ||
					(oldRotation==Surface.ROTATION_180 && rotation==Surface.ROTATION_270)){
				turnedClockwise=false;
			}
			//DebugUtility.log("rotation=%d %s",rotation,portraitIsNatural);
			//DebugUtility.log("clockwise=%s",turnedClockwise);
			if(oldWidth>width){ // landscape to portrait
				if(turnedClockwise){
					// turned clockwise
					mat=parallelogramMatrix(new RectF(0,0,oldWidth,oldHeight),
							new float[]{
						width,0,
						width,height,
						0,0
					});
				} else {
					// turned counterclockwise
					mat=parallelogramMatrix(new RectF(0,0,oldWidth,oldHeight),
							new float[]{
						0,height,
						0,0,
						width,height
					});
				}
			} else if(oldWidth<width){ // portrait to landscape
				if(turnedClockwise){
					// turned clockwise
					mat=parallelogramMatrix(new RectF(0,0,oldWidth,oldHeight),
							new float[]{
						width,0,
						width,height,
						0,0
					});
				} else {
					// turned counterclockwise
					mat=parallelogramMatrix(new RectF(0,0,oldWidth,oldHeight),
							new float[]{
						0,height,
						0,0,
						width,height
					});
				}
			} else {
				//DebugUtility.log("same width as before");
			}
			if(mat!=null){
				c.setMatrix(mat);
			}
			// use same coordinates, since we use a transform matrix
			c.drawBitmap(oldBitmap,
					new Rect(0,0,oldWidth,oldHeight),
					new RectF(0,0,oldWidth,oldHeight),null);
			if(oldBitmap!=null){
				oldBitmap.recycle();
			}
		} else {
			setOldRotation(AppManager.getRotation());
		}
		return newBitmap;
	}
}
