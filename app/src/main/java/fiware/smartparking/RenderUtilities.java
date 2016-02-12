package fiware.smartparking;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

import com.here.android.mpa.common.Image;

/**
 *   Map rendering utilities
 *
 *
 */
public class RenderUtilities {

    public static Image createLabeledIcon(Context ctx, String text1, float textSize, int textColor,
                                          int drawable) {
        try {
            Bitmap iconBitmap = BitmapFactory.decodeResource(ctx.getResources(), drawable);
            Paint paint = createPaint(ctx, textSize, textColor);
            float baseline = -paint.ascent();
            int textWidth = (int) (paint.measureText(text1) + 0.5f);
            int textHeight = (int) (baseline + paint.descent() + 0.5f);

            int width = Math.max(iconBitmap.getWidth(), textWidth);
            int height = iconBitmap.getHeight() + textHeight;
            Bitmap resBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas resCanvas = new Canvas(resBitmap);
            resCanvas.drawBitmap(iconBitmap, calculateLeft(width, iconBitmap.getWidth()), 0, null);
            resCanvas.drawText(text1, calculateLeft(width, textWidth),
                    baseline + iconBitmap.getHeight(), paint);

            Image resImage = new Image();
            resImage.setBitmap(resBitmap);
            return resImage;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Paint createPaint(Context ctx, float textSize,int textColor){
        Paint paint = new Paint();
        paint.setTextSize(dipToPixels(ctx, textSize));
        paint.setColor(textColor);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        return paint;
    }

    private static float dipToPixels(Context ctx, float dip){
        final float scale = ctx.getResources().getDisplayMetrics().density;
        return (dip * scale);
    }

    private static int calculateLeft (int globalWidth, int elementWidth){
        return (globalWidth - elementWidth)/2;
    }
}
