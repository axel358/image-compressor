package cu.axel.imagecompressor;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import android.os.Environment;
import android.graphics.BitmapFactory;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.graphics.Bitmap;
import java.text.DecimalFormat;
import android.app.AlertDialog;
import android.content.DialogInterface;
import java.io.ByteArrayOutputStream;
import android.graphics.Matrix;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.widget.RadioButton;
import android.graphics.Bitmap.CompressFormat;
import android.widget.SeekBar.OnSeekBarChangeListener;
import androidx.core.content.FileProvider;

public class MainActivity extends AppCompatActivity {
    private final int OPEN_REQUEST_CODE=4;
    private ContentResolver resolver;
    private ImageView previewIv;
    private Bitmap openBitmap;
    private TextView infoTv;
    private File compressedFile;
    private SharedPreferences sp;

    private SeekBar qualitySb, resSb;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        resolver = getContentResolver();
        previewIv = findViewById(R.id.preview_iv);
        infoTv = findViewById(R.id.image_info_tv);
        sp = PreferenceManager.getDefaultSharedPreferences(this);

        final RadioGroup formatGroup = findViewById(R.id.group_formats);
        qualitySb = findViewById(R.id.sb_quality);
        resSb = findViewById(R.id.sb_resolution);
        final TextView resTv = findViewById(R.id.tv_resolution);
        final TextView qualityTv = findViewById(R.id.tv_quality);

        qualitySb.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

                @Override
                public void onProgressChanged(SeekBar p1, int p2, boolean p3) {
                    qualityTv.setText(p2 + "");
                }

                @Override
                public void onStartTrackingTouch(SeekBar p1) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar p1) {
                    compress();
                }
            });
        resSb.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

                @Override
                public void onProgressChanged(SeekBar p1, int p2, boolean p3) {
                    resTv.setText(p2 + "");
                }

                @Override
                public void onStartTrackingTouch(SeekBar p1) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar p1) {
                    compress();
                }
            });
        resSb.setProgress(sp.getInt("resolution", 90));
        qualitySb.setProgress(sp.getInt("quality", 90));
        RadioButton webpBtn = findViewById(R.id.btn_webp);
        RadioButton jpgBtn = findViewById(R.id.btn_jpg);

        String format = sp.getString("format", "jpg");
        webpBtn.setChecked(format.equals("webp"));
        jpgBtn.setChecked(format.equals("jpg"));

        /*
         SharedPreferences.Editor editor = sp.edit();
         editor.putInt("resolution", resSb.getProgress());
         editor.putInt("quality", qualitySb.getProgress());
         RadioButton checkedBtn = view.findViewById(formatGroup.getCheckedRadioButtonId());
         editor.putString("format", checkedBtn.getText().toString().toLowerCase());
         editor.commit();*/

    }

    public void open(View v) {
        startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*"), OPEN_REQUEST_CODE);
    }

    public void showOptions(View v) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        final View view = getLayoutInflater().inflate(R.layout.dialog_options, null);
        dialog.setTitle("Compression options");
        dialog.setView(view);
        dialog.setNegativeButton("Cancel", null);


        dialog.setPositiveButton("Apply", new DialogInterface.OnClickListener(){

                @Override
                public void onClick(DialogInterface p1, int p2) {

                }
            });

        dialog.show();
    }

    public void share(View v) {
        Uri uri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".provider", compressedFile);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setData(uri);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(shareIntent, "Choose an app"));
    }

    public void compress() {

        if (openBitmap == null)
            return;

        try {

            //Scale
            Bitmap bitmap = scaleBitmap(openBitmap, resSb.getProgress() * 0.01f);

            //Compress
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            Bitmap.CompressFormat format = sp.getString("format", "jpg").equals("jpg") ? Bitmap.CompressFormat.JPEG : Bitmap.CompressFormat.WEBP;
            bitmap.compress(format, qualitySb.getProgress(), bytes);

            compressedFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "out.webp");
            compressedFile.createNewFile();
            FileOutputStream os = new FileOutputStream(compressedFile);
            os.write(bytes.toByteArray());
            os.close();

            infoTv.setText(bitmap.getWidth() + "x" + bitmap.getHeight() + " " + formatFileSize(compressedFile.length()));
            previewIv.setImageBitmap(BitmapFactory.decodeFile(compressedFile.getAbsolutePath()));

        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
        }


    }

    public Bitmap scaleBitmap(Bitmap bitmap, float scale) {
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == OPEN_REQUEST_CODE) {
                Uri openUri = data.getData();
                resolver.takePersistableUriPermission(openUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                //Read the file
                try {
                    InputStream is = resolver.openInputStream(openUri);
                    openBitmap = BitmapFactory.decodeStream(is);
                    is.close();
                    compress();
                } catch (IOException e) {}
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_select_image:
                open(null);
        }
        return super.onOptionsItemSelected(item);
    }

    public static String formatFileSize(long size) {
        if (size <= 0) return "0";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }


}
