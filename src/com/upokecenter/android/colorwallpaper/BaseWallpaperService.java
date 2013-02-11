/*
 * Modified by Peter O. from public domain code written
 * in 2012 by Markus Fisch <mf@markusfisch.de>
 * 
 * In the public domain.
 * 
 */
package com.upokecenter.android.colorwallpaper;

import android.graphics.Canvas;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;


public abstract class BaseWallpaperService extends WallpaperService
{
	protected abstract class Engine extends WallpaperService.Engine
	{
		protected int getDelay(){
			return 40;
		}

		final private Handler handler = new Handler();
		final private Runnable runnable = new Runnable()
		{
			public void run()
			{
				nextFrame();
			}
		};

		private boolean visible = false;
		private long time = 0;

		@Override
		public void onDestroy()
		{
			super.onDestroy();

			stopRunnable();
		}

		@Override
		public void onVisibilityChanged( boolean v )
		{
			visible = v;

			if( visible )
			{
				time = SystemClock.elapsedRealtime();
				nextFrame();
			}
			else
				stopRunnable();
		}

		/**
		 * @inheritDoc
		 */
		@Override
		public void onSurfaceChanged(
			SurfaceHolder holder,
			int format,
			int width,
			int height )
		{
			super.onSurfaceChanged( holder, format, width, height );

			nextFrame();
		}

		@Override
		public void onSurfaceDestroyed( SurfaceHolder holder )
		{
			visible = false;
			stopRunnable();

			super.onSurfaceDestroyed( holder );
		}

		@Override
		public void onOffsetsChanged(
			float xOffset,
			float yOffset,
			float xOffsetStep,
			float yOffsetStep,
			int xPixelOffset,
			int yPixelOffset )
		{
			nextFrame();
		}

		protected abstract void drawFrame( final Canvas c, final long e );

		protected abstract void onFrame();
		
		protected void nextFrame()
		{
			stopRunnable();

			if( !visible )
				return;

			onFrame();
			handler.postDelayed( runnable, getDelay() );
			
			final SurfaceHolder h = getSurfaceHolder();
			Canvas c = null;

			try
			{
				if( (c = h.lockCanvas()) != null )
				{
					final long now = SystemClock.elapsedRealtime();
					drawFrame( c, now-time );
					time = now;
				}
			}
			finally
			{
				if(c!=null && h!=null){
					try {
						h.unlockCanvasAndPost(c);
					} catch(IllegalArgumentException e){

					}
				}
			}
		}

		private void stopRunnable()
		{
			handler.removeCallbacks( runnable );
		}
	}
}
