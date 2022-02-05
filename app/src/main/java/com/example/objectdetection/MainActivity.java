package com.example.objectdetection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

/*import com.example.objectdetection.detection.ObjectDetectionAvtivity;*/
import com.example.objectdetection.helper.ImageHelperActivity;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.smarteist.autoimageslider.IndicatorView.animation.type.IndicatorAnimationType;
import com.smarteist.autoimageslider.SliderAnimations;
import com.smarteist.autoimageslider.SliderView;

public class MainActivity extends AppCompatActivity {
    SliderView sliderview;
    int[] images={R.drawable.one,
            R.drawable.two,
            R.drawable.three,
            R.drawable.four,
            R.drawable.six
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sliderview=findViewById(R.id.image_slider);

        SliderAdapter sliderAdapter=new SliderAdapter(images);
        sliderview.setSliderAdapter(sliderAdapter);
        sliderview.setIndicatorAnimation(IndicatorAnimationType.WORM);
        sliderview.setSliderTransformAnimation(SliderAnimations.DEPTHTRANSFORMATION);
        sliderview.startAutoCycle();
    }

    public void onGoToImageActivity(View vew){
        Intent intent = new Intent(this, ImageHelperActivity.class);
        startActivity(intent);

    }

}