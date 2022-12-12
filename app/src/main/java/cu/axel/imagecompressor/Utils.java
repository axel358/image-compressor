package cu.axel.imagecompressor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import java.text.DecimalFormat;

public class Utils {
    
    public static String formatFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
    
    public static Bitmap scaleBitmap(Bitmap bitmap, float scale) {
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }

}
