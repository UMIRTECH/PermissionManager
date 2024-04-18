package com.umirtech.permissionmanager;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Constraints;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.List;

public class PermissionManager {
    private final Context context;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private final PermissionManagerView pManagerView;

    private PermissionManagerView.OnSlideChangeListener slideChangeListener;

    private PermissionInfo selectedPermissionInfo;

    private int index = 0;
    private int preStatusBarColor;

    private boolean isNextAction = true;

    public PermissionManager(Context context) {
        this.context = context;
        pManagerView = new PermissionManagerView(context);
    }

    public void showPermissionManager(List<PermissionInfo> permissionsList,@NonNull PermissionResultCallBack resultCallBack) throws RuntimeException
    {
        if (context instanceof AppCompatActivity)
        {
            boolean isAllPermissionsGranted = false;
            for(PermissionInfo permissionInfo : permissionsList)
            {
                if (ContextCompat.checkSelfPermission(context, permissionInfo.getPermission()) != PackageManager.PERMISSION_GRANTED)
                {

                    if (checkVisualUserSelectedPermission(permissionInfo))
                    {
                        isAllPermissionsGranted = true;
                    }else {
                        isAllPermissionsGranted = false;
                        break;
                    }

                }else {
                    isAllPermissionsGranted = true;
                }
            }

            if (isAllPermissionsGranted)
            {
                resultCallBack.onPermissionsGranted();
                return;
            }


            AppCompatActivity activity = (AppCompatActivity) context;
            Window window = activity.getWindow();
            preStatusBarColor = window.getStatusBarColor();
            window.setStatusBarColor(pManagerView.darkBlue);

            ViewGroup contentView = activity.findViewById(android.R.id.content);
            ViewGroup rootView = (ViewGroup) contentView.getParent();
            rootView.removeView(contentView);

            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            rootView.addView(pManagerView,params);


            // Register permission request launcher
            requestPermissionLauncher = activity.registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted)
                        {
                            index++;
                            isNextAction = true;
                            String nextFabButtonText = "Next";
                            if (index >= permissionsList.size())
                            {
                                nextFabButtonText = "Finish";
                            }
                            pManagerView.nextFabButton.setText(nextFabButtonText);
                        }
                        else if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, selectedPermissionInfo.getPermission()))
                        {
                            // Permission is permanently denied
                            openAppSettings(activity);
                        }
                        else {
                            if (checkVisualUserSelectedPermission(selectedPermissionInfo))
                            {
                                index++;
                                isNextAction = true;
                                String nextFabButtonText = "Next";
                                if (index >= permissionsList.size())
                                {
                                    nextFabButtonText = "Finish";
                                }
                                pManagerView.nextFabButton.setText(nextFabButtonText);
                            }else {
                                // Permission is denied but not permanently
                            }

                        }
                    });

            if (slideChangeListener == null)
            {
                slideChangeListener = new PermissionManagerView.OnSlideChangeListener() {
                    @Override
                    public boolean onSlideChanged(TextView headerTextView, TextView permissionInfoTextView)
                    {
                        if (index < permissionsList.size() && index >= 0)
                        {
                            selectedPermissionInfo = permissionsList.get(index);
                            headerTextView.setText(selectedPermissionInfo.getPermissionTag());
                            permissionInfoTextView.setText(selectedPermissionInfo.getPermissionInfo());

                            if (ContextCompat.checkSelfPermission(context, selectedPermissionInfo.getPermission()) != PackageManager.PERMISSION_GRANTED)
                            {
                                if (checkVisualUserSelectedPermission(selectedPermissionInfo))
                                {
                                    index++;
                                    isNextAction = true;
                                    String nextFabButtonText = "Next";
                                    if (index >= permissionsList.size())
                                    {
                                        nextFabButtonText = "Finish";
                                    }
                                    pManagerView.nextFabButton.setText(nextFabButtonText);
                                }else {
                                    isNextAction = false;
                                    String nextFabButtonText = "Allow";
                                    pManagerView.nextFabButton.setText(nextFabButtonText);
                                }
                            }
                            else {
                                index++;
                                isNextAction = true;
                                String nextFabButtonText = "Next";
                                if (index >= permissionsList.size())
                                {
                                    nextFabButtonText = "Finish";
                                }
                                pManagerView.nextFabButton.setText(nextFabButtonText);
                            }
                        }else {
                            //// Hide Permission Manager ////
                            rootView.removeView(pManagerView);
                            rootView.addView(contentView);
                            requestPermissionLauncher.unregister();
                            window.setStatusBarColor(preStatusBarColor);
                            resultCallBack.onPermissionsGranted();
                            return false;
                        }

                        return true;
                    }
                };
            }

            pManagerView.nextFabButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isNextAction)
                    {
                        pManagerView.slideNext(slideChangeListener);
                    }else {
                        /// Ask Selected Permission ///
                        askPermission(selectedPermissionInfo);
                    }
                }
            });
        }else {
            throw new RuntimeException("Context is Not A Valid Activity");
        }
    }

    private void askPermission(PermissionInfo permissionInfo)
    {
        if (selectedPermissionInfo != null && requestPermissionLauncher != null)
        {
            requestPermissionLauncher.launch(permissionInfo.permission);
        }
    }

    private boolean checkVisualUserSelectedPermission(PermissionInfo permissionInfo)
    {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU)
        {
            if (permissionInfo.getPermission().equals(Manifest.permission.READ_MEDIA_VIDEO)
                    || permissionInfo.getPermission().equals(Manifest.permission.READ_MEDIA_AUDIO)
                    || permissionInfo.getPermission().equals(Manifest.permission.READ_MEDIA_IMAGES))
            {
                return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED;
            }
        }
        return false;
    }

    private void openAppSettings(Activity activity)
    {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }


    public static class PermissionInfo
    {
        private final String permission;
        private String permissionTag;
        private String permissionInfo;

        public PermissionInfo(String permission) {
            this.permission = permission;
        }

        public String getPermission() {
            return permission;
        }

        public String getPermissionTag() {
            return permissionTag;
        }

        public void setPermissionTag(String permissionTag) {
            this.permissionTag = permissionTag;
        }

        public String getPermissionInfo() {
            return permissionInfo;
        }

        public void setPermissionInfo(String permissionInfo) {
            this.permissionInfo = permissionInfo;
        }
    }


    public interface PermissionResultCallBack
    {
        void onPermissionsGranted();
        void onCancel();

    }


    /////// Inner Classes //////////

    private static class PermissionManagerView extends ConstraintLayout implements Animator.AnimatorListener
    {
        private Paint paint;
        private LinearGradient linearGradient;
        private int darkBlue,brightBlue,whiteTransparent;
        private String headerText,permissionInfoText,fabButtonText;

        ///// Child Views ////
        private TextView headerTextView, permissionInfoTextView;
        private ExtendedFloatingActionButton nextFabButton;

        public PermissionManagerView(Context context) {
            super(context);
            init(context);
        }

        public PermissionManagerView(Context context, @Nullable AttributeSet attrs) {
            super(context, attrs);
            init(context);
        }

        public PermissionManagerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init(context);
        }

        public PermissionManagerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
            init(context);
        }

        private void init(Context context)
        {
            setFitsSystemWindows(true);

            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.FILL);

            /// Color Parse ////
            darkBlue = Color.parseColor("#0033FF");
            brightBlue = Color.parseColor("#3F51B5");
            whiteTransparent = Color.parseColor("#20FFFFFF");


            headerTextView = genrateHeaderTextView(context);
            addView(headerTextView);
            addNextFabButton(context);

            permissionInfoTextView = genratePermissionInfoTextView(context);

            addView(permissionInfoTextView);

            invalidate();
            post(new Runnable() {
                @Override
                public void run() {
                    linearGradient = createLinearGradient(darkBlue,Color.WHITE,Color.WHITE,new float[]{0f, 0.9f, 1f});
                    invalidate();
                }
            });
        }



        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            super.onDraw(canvas);
        }


        @Override
        protected void dispatchDraw(Canvas canvas) {
            if (linearGradient != null)
            {
                paint.setShader(linearGradient);
                // Draw the gradient rectangle
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            }

            //// Draw ChildViews /////
            super.dispatchDraw(canvas);
        }


        private TextView genrateHeaderTextView(Context context)
        {
            //// Header Text Size ////
            int headerTextSizePx = dpToPx(context,10);
            headerText = "Welcome";

            TextView headerTextView = new TextView(context);
            int id = View.generateViewId();
            headerTextView.setId(id);
            headerTextView.setText(headerText);
            headerTextView.setTextColor(Color.WHITE);
            headerTextView.setTextSize(headerTextSizePx);
            headerTextView.setTypeface(null, Typeface.BOLD);

            LayoutParams params = new Constraints.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.topMargin = dpToPx(context,24);

            params.startToStart = LayoutParams.PARENT_ID;
            params.endToEnd = LayoutParams.PARENT_ID;
            params.topToTop = LayoutParams.PARENT_ID;

            headerTextView.setLayoutParams(params);

            return headerTextView;
        }

        private void addNextFabButton(Context context)
        {
            //// Header Text Size ////
            fabButtonText = "NEXT";

            nextFabButton = new ExtendedFloatingActionButton(context);
            int id = View.generateViewId();
            nextFabButton.setId(id);
            nextFabButton.setText(fabButtonText);
            nextFabButton.setTextColor(Color.WHITE);
            nextFabButton.setTypeface(null, Typeface.BOLD);
            nextFabButton.setTextAlignment(TEXT_ALIGNMENT_CENTER);

            ColorStateList fabTint = ColorStateList.valueOf(brightBlue);
            nextFabButton.setBackgroundTintList(fabTint);

            LayoutParams params = new Constraints.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dpToPx(context,30);
            params.rightMargin = dpToPx(context,30);

            params.endToEnd = LayoutParams.PARENT_ID;
            params.bottomToBottom = LayoutParams.PARENT_ID;
            addView(nextFabButton,params);
        }

        private TextView genratePermissionInfoTextView(Context context)
        {
            //// Header Text Size ////
            int textSizePx = dpToPx(context,7);
            permissionInfoText = "You need to Grant Some Required Permissions to this App";

            TextView permissionInfoTextView = new TextView(context);
            int id = View.generateViewId();
            permissionInfoTextView.setId(id);

            permissionInfoTextView.setText(permissionInfoText);

            permissionInfoTextView.setTextColor(Color.WHITE);
            permissionInfoTextView.setTextSize(textSizePx);
            permissionInfoTextView.setTypeface(null, Typeface.BOLD);
            permissionInfoTextView.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            int padding = dpToPx(context, 20);
            permissionInfoTextView.setPadding(padding,padding,padding,padding);
            setRoundedCornerBackground(permissionInfoTextView,whiteTransparent,dpToPx(context,30));

            LayoutParams params = new Constraints.LayoutParams(LayoutParams.MATCH_CONSTRAINT, LayoutParams.MATCH_CONSTRAINT);
            params.topMargin = dpToPx(context,8);
            params.rightMargin = dpToPx(context,20);
            params.leftMargin = dpToPx(context,20);

            params.startToStart = LayoutParams.PARENT_ID;
            params.endToEnd = LayoutParams.PARENT_ID;
            params.topToBottom = headerTextView.getId();
            params.bottomToBottom = LayoutParams.PARENT_ID;

            permissionInfoTextView.setLayoutParams(params);

            return permissionInfoTextView;
        }

        private boolean isSlideNext = true;
        private OnSlideChangeListener slideChangeListener;
        private void slideNext(@Nullable OnSlideChangeListener slideChangeListener)
        {
            this.slideChangeListener = slideChangeListener;
            isSlideNext = true;
            animateViewTranslationXY(headerTextView,1100,0,800,new AnticipateInterpolator(),null);
            animateViewTranslationXY(permissionInfoTextView,1100,0,1000,new AnticipateInterpolator(),this);
        }

        private void slidePre(@Nullable OnSlideChangeListener slideChangeListener)
        {
            this.slideChangeListener = slideChangeListener;
            isSlideNext = false;
            animateViewTranslationXY(headerTextView,-1100,0,800,new AnticipateInterpolator(),null);
            animateViewTranslationXY(permissionInfoTextView,-1100,0,1000,new AnticipateInterpolator(),this);
        }

        private LinearGradient createLinearGradient(int startColor, int middleColor, int endColor, float[] positions) {
            // Define gradient colors
            int[] colors = {startColor, middleColor, endColor};
            return new LinearGradient(
                    0, 0, // Start point
                    0, getHeight(),     // End point
                    colors,         // Gradient colors
                    positions,      // Gradient positions
                    Shader.TileMode.CLAMP // Shader tiling mode
            );
        }

        public void setRoundedCornerBackground(View view, int backgroundColor, float cornerRadius)
        {
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setColor(backgroundColor);
            shape.setCornerRadius(cornerRadius);
            view.setBackground(shape);
        }
        private int dpToPx(Context context, float dp) {
            return (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    dp,
                    context.getResources().getDisplayMetrics()
            );
        }


        private void animateViewTranslationXY(View view, float translationX, float translationY, long duration,
                                              @Nullable Interpolator interpolator, @Nullable Animator.AnimatorListener animatorListener)
        {
            ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(
                    view,
                    PropertyValuesHolder.ofFloat(View.TRANSLATION_X, translationX),
                    PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, translationY)
            );

            if (animatorListener != null)
            {
                animator.addListener(animatorListener);
            }


            if (interpolator != null)
            {
                animator.setInterpolator(interpolator);
            }
            animator.setDuration(duration);
            animator.start();
        }

        @Override
        public void onAnimationStart(@NonNull Animator animation) {

        }

        @Override
        public void onAnimationEnd(@NonNull Animator animation) {
            boolean canShowInAnimation = true;
            if (slideChangeListener != null)
            {
                canShowInAnimation = slideChangeListener.onSlideChanged(headerTextView,permissionInfoTextView);
            }
            if (canShowInAnimation)
            {
                if (isSlideNext)
                {
                    headerTextView.setTranslationX(-1100);
                    permissionInfoTextView.setTranslationX(-1100);

                }else {
                    headerTextView.setTranslationX(1100);
                    permissionInfoTextView.setTranslationX(1100);

                }
                animateViewTranslationXY(headerTextView,0,0,800,new OvershootInterpolator(),null);
                animateViewTranslationXY(permissionInfoTextView,0,0,1000,new OvershootInterpolator(),null);
            }
        }

        @Override
        public void onAnimationCancel(@NonNull Animator animation) {

        }

        @Override
        public void onAnimationRepeat(@NonNull Animator animation) {

        }


        private interface OnSlideChangeListener
        {
            boolean onSlideChanged(TextView headerTextView,TextView permissionInfoTextView);
        }


    }

}
