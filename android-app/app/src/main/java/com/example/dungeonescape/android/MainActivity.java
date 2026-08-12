package com.example.dungeonescape.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.example.dungeonescape.model.Direction;
import com.example.dungeonescape.model.GameState;
import com.example.dungeonescape.model.Position;
import com.example.dungeonescape.model.entity.Boulder;
import com.example.dungeonescape.model.entity.Door;
import com.example.dungeonescape.model.entity.Entity;
import com.example.dungeonescape.model.entity.Exit;
import com.example.dungeonescape.model.entity.FloorSwitch;
import com.example.dungeonescape.model.entity.Key;
import com.example.dungeonescape.model.entity.Player;
import com.example.dungeonescape.model.entity.Treasure;
import com.example.dungeonescape.model.entity.Wall;
import com.example.dungeonescape.persistence.InvalidLevelException;
import com.example.dungeonescape.service.GameEngine;
import com.example.dungeonescape.service.MoveResult;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Native Android shell that reuses the original game engine and level data. */
public final class MainActivity extends Activity {
    private static final int GOLD = Color.rgb(216, 169, 74);
    private static final int BLUE = Color.rgb(44, 105, 190);
    private static final String[] LEVEL_NAMES = {
            "基础迷宫", "宝物猎人", "钥匙与门", "巨石机关", "终极试炼"
    };
    private static final String[] LEVEL_SUBTITLES = {
            "找到出口 · 熟悉移动", "收集 3 件宝物", "取得钥匙 · 打开石门",
            "推动巨石 · 压住机关", "宝物、机关与出口"
    };

    private int currentLevel = 1;
    private GameBoardView gameView;
    private FeedbackSystem feedback;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        feedback = new FeedbackSystem();
        showMainMenu();
    }

    @Override
    protected void onDestroy() {
        if (feedback != null) feedback.release();
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (gameView != null) gameView.requestLayout();
    }

    private void showMainMenu() {
        gameView = null;
        setContentView(new MainMenuSkinView(this));
    }

    private void showLevelSelect() {
        gameView = null;
        setContentView(new LevelSelectSkinView(this));
    }

    private void startLevel(int level) {
        currentLevel = level;
        try (InputStream in = getAssets().open("levels/level" + level + ".json")) {
            GameState state = AndroidLevelLoader.load(in, "assets/levels/level" + level + ".json");
            gameView = new GameBoardView(this, level, state);
            setContentView(gameView);
            gameView.requestFocus();
        } catch (IOException | InvalidLevelException error) {
            new AlertDialog.Builder(this)
                    .setTitle("关卡加载失败")
                    .setMessage(error.getMessage())
                    .setPositiveButton("返回", (dialog, which) -> showLevelSelect())
                    .show();
        }
    }

    private void restartLevel() {
        startLevel(currentLevel);
    }

    private void showSettings() {
        boolean[] enabled = {feedback.isSoundEnabled(), feedback.isVibrationEnabled()};
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("声音与震动")
                .setMultiChoiceItems(new String[]{"声音", "震动"}, enabled, (ignored, which, checked) -> {
                    if (which == 0) feedback.setSoundEnabled(checked);
                    else feedback.setVibrationEnabled(checked);
                    feedback.click(getWindow().getDecorView());
                })
                .setPositiveButton("完成", null)
                .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(view -> {
                    feedback.click(view);
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private void showVictory(GameState state) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(new VictorySkinView(this, dialog, state),
                new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams attributes = window.getAttributes();
            attributes.dimAmount = .76f;
            window.setAttributes(attributes);
        }
        dialog.show();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private LinearLayout basePanel() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(8, 12, 22), Color.rgb(14, 23, 39), Color.rgb(8, 12, 22)}));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int left = insets.getSystemWindowInsetLeft();
            int top = insets.getSystemWindowInsetTop();
            int right = insets.getSystemWindowInsetRight();
            int bottom = insets.getSystemWindowInsetBottom();
            view.setPadding(dp(24) + left, dp(24) + top, dp(24) + right, dp(24) + bottom);
            return insets;
        });
        return root;
    }

    private TextView title(String text, int sp) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(GOLD);
        view.setGravity(Gravity.CENTER);
        view.setLetterSpacing(0.04f);
        view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return view;
    }

    private Button actionButton(String text, boolean primary, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(17);
        button.setTextColor(Color.WHITE);
        button.setAllCaps(false);
        button.setMinHeight(dp(54));
        button.setBackground(buttonSelector(primary));
        button.setOnClickListener(listener);
        return button;
    }

    private StateListDrawable buttonSelector(boolean primary) {
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_pressed},
                rounded(primary ? Color.rgb(35, 86, 160) : Color.rgb(49, 60, 86),
                        GOLD, 14, 2));
        states.addState(new int[0],
                rounded(primary ? BLUE : Color.rgb(31, 40, 61),
                        primary ? Color.rgb(82, 145, 228) : Color.rgb(65, 79, 111), 14, 1));
        return states;
    }

    private GradientDrawable rounded(int fill, int stroke, float radiusDp, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(strokeDp), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = margins(
                Math.min(dp(420), getResources().getDisplayMetrics().widthPixels - dp(48)), dp(58), 13);
        p.gravity = Gravity.CENTER_HORIZONTAL;
        return p;
    }

    private LinearLayout.LayoutParams margins(int width, int height, int bottomDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height);
        p.bottomMargin = dp(bottomDp);
        return p;
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class FeedbackSystem {
        private static final String PREFS = "dungeon_feedback";
        private static final String SOUND = "sound_enabled";
        private static final String VIBRATION = "vibration_enabled";

        private final SharedPreferences preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        private ToneGenerator tones;
        private boolean soundEnabled = preferences.getBoolean(SOUND, true);
        private boolean vibrationEnabled = preferences.getBoolean(VIBRATION, false);

        FeedbackSystem() {
            try {
                tones = new ToneGenerator(AudioManager.STREAM_MUSIC, 58);
            } catch (RuntimeException ignored) {
                tones = null;
            }
        }

        boolean isSoundEnabled() { return soundEnabled; }
        boolean isVibrationEnabled() { return vibrationEnabled; }

        void setSoundEnabled(boolean enabled) {
            soundEnabled = enabled;
            preferences.edit().putBoolean(SOUND, enabled).apply();
        }

        void setVibrationEnabled(boolean enabled) {
            vibrationEnabled = enabled;
            preferences.edit().putBoolean(VIBRATION, enabled).apply();
        }

        void click(View view) {
            if (soundEnabled) view.playSoundEffect(SoundEffectConstants.CLICK);
            vibrate(view, HapticFeedbackConstants.KEYBOARD_TAP);
        }

        void move(View view, MoveResult result) {
            if (soundEnabled) {
                if (!result.moved()) playTone(ToneGenerator.TONE_PROP_NACK, 85);
                else if (result.keyCollected() || result.treasureCollected())
                    playTone(ToneGenerator.TONE_PROP_ACK, 120);
                else if (result.doorOpened()) playTone(ToneGenerator.TONE_PROP_PROMPT, 110);
                else if (result.boulderPushed()) playTone(ToneGenerator.TONE_PROP_BEEP2, 80);
                else playTone(ToneGenerator.TONE_PROP_BEEP, 42);
            }
            vibrate(view, result.moved()
                    ? HapticFeedbackConstants.KEYBOARD_TAP
                    : HapticFeedbackConstants.LONG_PRESS);
        }

        void victory(View view) {
            if (soundEnabled) playTone(ToneGenerator.TONE_PROP_ACK, 260);
            vibrate(view, HapticFeedbackConstants.LONG_PRESS);
        }

        private void playTone(int tone, int durationMs) {
            if (tones != null) tones.startTone(tone, durationMs);
        }

        private void vibrate(View view, int type) {
            if (vibrationEnabled) view.performHapticFeedback(type);
        }

        void release() {
            if (tones != null) {
                tones.release();
                tones = null;
            }
        }
    }

    private boolean pathContains(Path path, float x, float y) {
        RectF bounds = new RectF();
        path.computeBounds(bounds, true);
        Region clip = new Region((int) Math.floor(bounds.left), (int) Math.floor(bounds.top),
                (int) Math.ceil(bounds.right), (int) Math.ceil(bounds.bottom));
        Region region = new Region();
        region.setPath(path, clip);
        return region.contains(Math.round(x), Math.round(y));
    }

    private abstract class SkinScreenView extends View {
        final Paint skinPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Bitmap skin;
        final RectF skinRect = new RectF();
        int insetLeft, insetTop, insetRight, insetBottom;

        SkinScreenView(Context context, String assetName, String description) {
            super(context);
            Bitmap loaded = null;
            try (InputStream in = getAssets().open("images/" + assetName)) {
                loaded = BitmapFactory.decodeStream(in);
            } catch (IOException ignored) { }
            skin = loaded;
            setContentDescription(description);
            setFocusable(true);
            setBackgroundColor(Color.rgb(4, 8, 15));
        }

        @Override public WindowInsets onApplyWindowInsets(WindowInsets insets) {
            insetLeft = insets.getSystemWindowInsetLeft();
            insetTop = insets.getSystemWindowInsetTop();
            insetRight = insets.getSystemWindowInsetRight();
            insetBottom = insets.getSystemWindowInsetBottom();
            invalidate();
            return insets;
        }

        @Override protected final void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float left = insetLeft;
            float top = insetTop;
            float width = getWidth() - insetLeft - insetRight;
            float height = getHeight() - insetTop - insetBottom;
            if (skin == null || width <= 0 || height <= 0) return;
            float scale = Math.min(width / skin.getWidth(), height / skin.getHeight());
            float drawWidth = skin.getWidth() * scale;
            float drawHeight = skin.getHeight() * scale;
            float drawLeft = left + (width - drawWidth) / 2f;
            float drawTop = top + (height - drawHeight) / 2f;
            skinRect.set(drawLeft, drawTop, drawLeft + drawWidth, drawTop + drawHeight);
            canvas.drawBitmap(skin, null, skinRect, skinPaint);
            drawLiveContent(canvas);
        }

        abstract void drawLiveContent(Canvas canvas);

        void text(Canvas canvas, String value, float nx, float ny, float sp, int color, boolean bold) {
            textPaint.setColor(color);
            textPaint.setTextSize(sp * getResources().getDisplayMetrics().scaledDensity);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(bold ? android.graphics.Typeface.DEFAULT_BOLD
                    : android.graphics.Typeface.DEFAULT);
            canvas.drawText(value, px(nx), py(ny), textPaint);
        }

        void textCenteredIn(Canvas canvas, String value, RectF rect, float sp, int color, boolean bold) {
            textPaint.setColor(color);
            textPaint.setTextSize(sp * getResources().getDisplayMetrics().scaledDensity);
            textPaint.setTextAlign(Paint.Align.CENTER);
            textPaint.setTypeface(bold ? android.graphics.Typeface.DEFAULT_BOLD
                    : android.graphics.Typeface.DEFAULT);
            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            float baseline = rect.centerY() - (metrics.ascent + metrics.descent) / 2f;
            canvas.drawText(value, rect.centerX(), baseline, textPaint);
        }

        float px(float normalized) { return skinRect.left + skinRect.width() * normalized; }
        float py(float normalized) { return skinRect.top + skinRect.height() * normalized; }
        RectF area(float l, float t, float r, float b) {
            return new RectF(px(l), py(t), px(r), py(b));
        }

        void drawPressedPanel(Canvas canvas, RectF rect) {
            // Rendering and hit-testing intentionally share this exact path. There is no
            // independent rectangle or inset that can drift away from the button artwork.
            Path exactButton = panelPath(rect);
            textPaint.setStyle(Paint.Style.FILL);
            textPaint.setColor(Color.argb(148, 73, 225, 236));
            canvas.drawPath(exactButton, textPaint);
        }

        Path panelPath(RectF rect) {
            float cut = Math.min(rect.width() * .08f, rect.height() * .28f);
            Path path = new Path();
            path.moveTo(rect.left + cut, rect.top);
            path.lineTo(rect.right - cut, rect.top);
            path.lineTo(rect.right, rect.top + cut);
            path.lineTo(rect.right, rect.bottom - cut);
            path.lineTo(rect.right - cut, rect.bottom);
            path.lineTo(rect.left + cut, rect.bottom);
            path.lineTo(rect.left, rect.bottom - cut);
            path.lineTo(rect.left, rect.top + cut);
            path.close();
            return path;
        }

        boolean panelContains(RectF rect, float x, float y) {
            return pathContains(panelPath(rect), x, y);
        }

        @Override public boolean performClick() {
            super.performClick();
            return true;
        }
    }

    private final class MainMenuSkinView extends SkinScreenView {
        private final RectF start = new RectF();
        private final RectF resume = new RectF();
        private final RectF settings = new RectF();
        private RectF pressed;

        MainMenuSkinView(Context context) {
            super(context, "main-menu-skin.png", "地牢逃脱主菜单");
        }

        @Override void drawLiveContent(Canvas canvas) {
            text(canvas, "MOBILE EDITION", .5f, .103f, 11, Color.rgb(113, 203, 219), true);
            text(canvas, "DUNGEON ESCAPE", .5f, .139f, 25, GOLD, true);
            text(canvas, "暗影地牢 · 手机版", .5f, .178f, 15, Color.rgb(194, 205, 225), true);
            text(canvas, "五座地牢 · 一条生路", .5f, .365f, 18, Color.WHITE, true);
            text(canvas, "滑动棋盘，解开机关，找到出口", .5f, .405f, 14,
                    Color.rgb(180, 195, 216), false);
            start.set(area(.160f, .585f, .840f, .665f));
            resume.set(area(.160f, .718f, .840f, .798f));
            settings.set(area(.225f, .850f, .775f, .915f));
            textCenteredIn(canvas, "开始游戏", start, 20,
                    pressed == start ? Color.rgb(111, 237, 242) : Color.WHITE, true);
            textCenteredIn(canvas, "继续第 " + currentLevel + " 关", resume, 18,
                    pressed == resume ? Color.rgb(111, 237, 242) : Color.WHITE, true);
            textCenteredIn(canvas, "设置", settings, 17,
                    pressed == settings ? Color.rgb(111, 237, 242) : Color.WHITE, true);
            if (pressed != null) drawPressedPanel(canvas, pressed);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                pressed = panelContains(start, event.getX(), event.getY()) ? start
                        : panelContains(resume, event.getX(), event.getY()) ? resume
                        : panelContains(settings, event.getX(), event.getY()) ? settings : null;
                if (pressed != null) {
                    feedback.click(this);
                }
                invalidate();
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                RectF hovered = panelContains(start, event.getX(), event.getY()) ? start
                        : panelContains(resume, event.getX(), event.getY()) ? resume
                        : panelContains(settings, event.getX(), event.getY()) ? settings : null;
                if (hovered != pressed) {
                    pressed = hovered;
                    invalidate();
                }
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                pressed = null;
                invalidate();
                return true;
            }
            if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
            performClick();
            RectF released = pressed;
            pressed = null;
            invalidate();
            if (released == start && panelContains(start, event.getX(), event.getY())) showLevelSelect();
            else if (released == resume && panelContains(resume, event.getX(), event.getY())) startLevel(currentLevel);
            else if (released == settings && panelContains(settings, event.getX(), event.getY())) showSettings();
            return true;
        }

        @Override public boolean performClick() {
            return super.performClick();
        }
    }

    private final class LevelSelectSkinView extends SkinScreenView {
        private final RectF[] rows = {new RectF(), new RectF(), new RectF(), new RectF(), new RectF()};
        private final RectF back = new RectF();
        private RectF pressed;

        LevelSelectSkinView(Context context) {
            super(context, "level-select-skin.png", "选择五个地牢关卡");
        }

        @Override void drawLiveContent(Canvas canvas) {
            text(canvas, "DUNGEON MAP", .5f, .069f, 12, Color.rgb(111, 224, 190), true);
            text(canvas, "选择关卡", .5f, .099f, 25, GOLD, true);
            text(canvas, "每一关都可独立挑战", .5f, .125f, 12,
                    Color.rgb(177, 191, 214), false);
            float[] centers = {.238f, .382f, .526f, .670f, .812f};
            float half = .045f;
            for (int i = 0; i < 5; i++) {
                text(canvas, String.format(Locale.CHINA, "%02d  %s", i + 1, LEVEL_NAMES[i]),
                        .5f, centers[i] - .008f, 17,
                        pressed == rows[i] ? Color.rgb(111, 237, 242)
                                : i == currentLevel - 1 ? GOLD : Color.WHITE, true);
                text(canvas, LEVEL_SUBTITLES[i], .5f, centers[i] + .025f, 12,
                        Color.rgb(178, 200, 216), false);
                rows[i].set(area(.105f, centers[i] - half, .895f, centers[i] + half));
            }
            back.set(area(.145f, .913f, .855f, .974f));
            textCenteredIn(canvas, "返回主菜单", back, 18,
                    pressed == back ? Color.rgb(111, 237, 242) : Color.WHITE, true);
            if (pressed != null) drawPressedPanel(canvas, pressed);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                pressed = null;
                for (RectF row : rows) if (panelContains(row, event.getX(), event.getY())) pressed = row;
                if (panelContains(back, event.getX(), event.getY())) pressed = back;
                if (pressed != null) {
                    feedback.click(this);
                }
                invalidate();
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                RectF hovered = null;
                for (RectF row : rows) {
                    if (panelContains(row, event.getX(), event.getY())) hovered = row;
                }
                if (panelContains(back, event.getX(), event.getY())) hovered = back;
                if (hovered != pressed) {
                    pressed = hovered;
                    invalidate();
                }
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                pressed = null;
                invalidate();
                return true;
            }
            if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
            performClick();
            RectF released = pressed;
            pressed = null;
            invalidate();
            for (int i = 0; i < rows.length; i++) {
                if (released == rows[i] && panelContains(rows[i], event.getX(), event.getY())) {
                    startLevel(i + 1);
                    return true;
                }
            }
            if (released == back && panelContains(back, event.getX(), event.getY())) showMainMenu();
            return true;
        }

        @Override public boolean performClick() {
            return super.performClick();
        }
    }

    /** Fixed-aspect, skinned victory dialog with four large touch targets. */
    private final class VictorySkinView extends SkinScreenView {
        private final Dialog dialog;
        private final GameState completedState;
        private final boolean hasNext;
        private final RectF retry = new RectF();
        private final RectF select = new RectF();
        private final RectF mainMenu = new RectF();
        private final RectF next = new RectF();
        private RectF pressed;

        VictorySkinView(Context context, Dialog dialog, GameState completedState) {
            super(context, "victory-dialog-skin.png", "关卡完成操作面板");
            this.dialog = dialog;
            this.completedState = completedState;
            this.hasNext = currentLevel < LEVEL_NAMES.length;
            setBackgroundColor(Color.TRANSPARENT);
        }

        @Override void drawLiveContent(Canvas canvas) {
            String heading = hasNext ? "关卡完成" : "全部通关";
            String result = "你用了 " + completedState.getMoveCount() + " 步完成“"
                    + LEVEL_NAMES[currentLevel - 1] + "”";
            text(canvas, heading, .5f, .255f, 27, GOLD, true);
            text(canvas, result, .5f, .425f, 15, Color.rgb(205, 221, 228), false);
            text(canvas, "重新挑战", .165f, .835f, 12, Color.rgb(112, 225, 230), true);
            text(canvas, "选择关卡", .388f, .835f, 12, Color.rgb(112, 225, 230), true);
            text(canvas, "返回主菜单", .612f, .835f, 11, Color.rgb(112, 225, 230), true);
            text(canvas, hasNext ? "下一关" : "完成", .840f, .835f, 12, GOLD, true);

            retry.set(area(.055f, .725f, .275f, .940f));
            select.set(area(.278f, .725f, .500f, .940f));
            mainMenu.set(area(.502f, .725f, .725f, .940f));
            next.set(area(.728f, .725f, .950f, .940f));
            if (pressed != null) drawPressedPanel(canvas, pressed);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                pressed = panelContains(retry, event.getX(), event.getY()) ? retry
                        : panelContains(select, event.getX(), event.getY()) ? select
                        : panelContains(mainMenu, event.getX(), event.getY()) ? mainMenu
                        : panelContains(next, event.getX(), event.getY()) ? next : null;
                if (pressed != null) {
                    feedback.click(this);
                }
                invalidate();
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                RectF hovered = panelContains(retry, event.getX(), event.getY()) ? retry
                        : panelContains(select, event.getX(), event.getY()) ? select
                        : panelContains(mainMenu, event.getX(), event.getY()) ? mainMenu
                        : panelContains(next, event.getX(), event.getY()) ? next : null;
                if (hovered != pressed) {
                    pressed = hovered;
                    invalidate();
                }
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                pressed = null;
                invalidate();
                return true;
            }
            if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
            performClick();
            float x = event.getX();
            float y = event.getY();
            RectF released = pressed;
            pressed = null;
            invalidate();
            if (released == retry && panelContains(retry, x, y)) {
                dialog.dismiss();
                restartLevel();
            } else if (released == select && panelContains(select, x, y)) {
                dialog.dismiss();
                showLevelSelect();
            } else if (released == mainMenu && panelContains(mainMenu, x, y)) {
                dialog.dismiss();
                showMainMenu();
            } else if (released == next && panelContains(next, x, y)) {
                dialog.dismiss();
                if (hasNext) startLevel(currentLevel + 1);
                else showMainMenu();
            }
            return true;
        }
    }

    /** One responsive, hardware-accelerated view for the board, HUD and touch controls. */
    private final class GameBoardView extends View {
        private static final int SHEET_CELL = 362;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pixelPaint = new Paint();
        private final int level;
        private final GameState state;
        private final GameEngine engine;
        private final int initialTreasures;
        private final Bitmap sprites;
        private final Bitmap uiSkin;
        private final Bitmap hudSkin;
        private final RectF up = new RectF(), down = new RectF(), left = new RectF(), right = new RectF();
        private final RectF restart = new RectF(), levels = new RectF(), menu = new RectF();
        private final RectF upVisual = new RectF(), downVisual = new RectF();
        private final RectF leftVisual = new RectF(), rightVisual = new RectF();
        private final RectF restartVisual = new RectF(), levelsVisual = new RectF();
        private final Path upShape = new Path(), downShape = new Path();
        private final Path leftShape = new Path(), rightShape = new Path();
        private final Path restartShape = new Path(), levelsShape = new Path();
        private float touchX, touchY;
        private int safeLeft, safeTop, safeRight, safeBottom;
        private RectF pressedTarget;
        private boolean skinnedControlsActive;

        GameBoardView(Context context, int level, GameState state) {
            super(context);
            this.level = level;
            this.state = state;
            this.engine = new GameEngine(state);
            this.initialTreasures = state.countEntities(Treasure.class);
            this.pixelPaint.setAntiAlias(false);
            this.pixelPaint.setFilterBitmap(false);
            Bitmap loaded = null;
            try (InputStream in = getAssets().open("images/dungeon-spritesheet.png")) {
                loaded = BitmapFactory.decodeStream(in);
            } catch (IOException ignored) {
                // Shape fallbacks below keep the game playable if the bitmap is unavailable.
            }
            this.sprites = loaded;
            Bitmap skin = null;
            try (InputStream in = getAssets().open("images/game-console-unified-v2.png")) {
                skin = BitmapFactory.decodeStream(in);
            } catch (IOException ignored) {
                // The vector-free native fallback remains usable if the optional skin is unavailable.
            }
            this.uiSkin = skin;
            Bitmap headerSkin = null;
            try (InputStream in = getAssets().open("images/level-select-skin.png")) {
                headerSkin = BitmapFactory.decodeStream(in);
            } catch (IOException ignored) {
                // The painted fallback remains usable if the texture is unavailable.
            }
            this.hudSkin = headerSkin;
            this.engine.addEventListener(won -> post(() -> {
                feedback.victory(GameBoardView.this);
                showVictory(won);
            }));
            setFocusable(true);
            setContentDescription("地牢棋盘。可在棋盘上滑动，或点击屏幕下方方向键移动。");
            setBackgroundColor(Color.rgb(9, 13, 24));
        }

        @Override
        public WindowInsets onApplyWindowInsets(WindowInsets insets) {
            safeLeft = insets.getSystemWindowInsetLeft();
            safeTop = insets.getSystemWindowInsetTop();
            safeRight = insets.getSystemWindowInsetRight();
            safeBottom = insets.getSystemWindowInsetBottom();
            invalidate();
            return insets;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth(), h = getHeight();
            float contentLeft = safeLeft;
            float contentTop = safeTop;
            float contentRight = w - safeRight;
            float contentBottom = h - safeBottom;
            float contentWidth = contentRight - contentLeft;
            boolean landscape = contentWidth > contentBottom - contentTop;
            float sidePanel = landscape ? Math.min(contentWidth * 0.30f, dp(310)) : 0;
            float boardAreaLeft = contentLeft + (landscape ? dp(18) : dp(8));
            float boardAreaRight = landscape ? contentRight - sidePanel - dp(18) : contentRight - dp(8);
            float header = contentTop + (landscape ? dp(116) : dp(190));
            float controlTop = landscape ? contentTop + dp(102) : contentBottom - dp(194);
            float boardBottom = landscape ? contentBottom - dp(18) : controlTop - dp(12);

            drawBackground(canvas, w, h);
            drawUnifiedShell(canvas, contentLeft, contentTop, contentRight, contentBottom);
            drawHeader(canvas, boardAreaLeft, boardAreaRight, landscape);

            float cell = Math.min((boardAreaRight - boardAreaLeft - dp(12)) / state.getWidth(),
                    (boardBottom - header - dp(8)) / state.getHeight());
            cell = Math.max(dp(18), cell);
            float boardW = cell * state.getWidth();
            float boardH = cell * state.getHeight();
            float bx = boardAreaLeft + (boardAreaRight - boardAreaLeft - boardW) / 2f;
            float by = landscape
                    ? header + Math.max(0, (boardBottom - header - boardH) / 2f)
                    : header;
            drawBoard(canvas, bx, by, cell);

            if (landscape) {
                drawControls(canvas, contentRight - sidePanel, contentTop + dp(82),
                        sidePanel - dp(12), contentBottom - contentTop - dp(176));
                drawNavigation(canvas, contentRight - sidePanel, contentBottom - dp(82),
                        sidePanel - dp(12), dp(68));
            } else {
                float consoleTop = by + boardH;
                drawControls(canvas, contentLeft + dp(4), consoleTop,
                        contentWidth - dp(8), contentBottom - consoleTop);
            }
        }

        private void drawBackground(Canvas canvas, int w, int h) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(12, 19, 28));
            canvas.drawRect(0, 0, w, h, paint);
        }

        private void drawUnifiedShell(Canvas canvas, float left, float top, float right, float bottom) {
            if (hudSkin == null) return;
            drawNineSlice(canvas, hudSkin, new Rect(54, 49, 788, 282),
                    new RectF(left, top + dp(7), right, bottom), 54);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(190, 5, 11, 19));
            canvas.drawRect(left + dp(8), top + dp(25), right - dp(8), bottom - dp(8), paint);
        }

        private void drawHeader(Canvas canvas, float leftEdge, float rightEdge, boolean landscape) {
            float center = (leftEdge + rightEdge) / 2f;
            float top = safeTop;
            float panelBottom = top + (landscape ? dp(112) : dp(188));
            paint.setStyle(Paint.Style.FILL);
            if (hudSkin != null) {
                RectF titlePlate = new RectF(leftEdge + dp(4), top + dp(24),
                        rightEdge - dp(4), top + (landscape ? dp(69) : dp(96)));
                drawNineSlice(canvas, hudSkin, new Rect(70, 79, 772, 278), titlePlate, 48);
                paint.setColor(Color.argb(184, 5, 11, 19));
                canvas.drawRoundRect(titlePlate.left + dp(6), titlePlate.top + dp(5),
                        titlePlate.right - dp(6), titlePlate.bottom - dp(5), dp(7), dp(7), paint);
            } else {
                paint.setColor(Color.argb(225, 16, 24, 42));
                canvas.drawRoundRect(leftEdge, top + dp(7), rightEdge, panelBottom,
                        dp(14), dp(14), paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(1));
                paint.setColor(Color.rgb(48, 62, 91));
                canvas.drawRoundRect(leftEdge, top + dp(7), rightEdge, panelBottom,
                        dp(14), dp(14), paint);
                paint.setStyle(Paint.Style.FILL);
            }
            drawText(canvas, String.format(Locale.CHINA, "%02d  %s", level, LEVEL_NAMES[level - 1]),
                    center, top + (landscape ? dp(38) : dp(53)), landscape ? 18 : 20,
                    GOLD, Paint.Align.CENTER, true);
            drawText(canvas, "v2.1.0", rightEdge - dp(17), top + dp(39), 9,
                    Color.rgb(106, 125, 148), Paint.Align.RIGHT, false);
            canvas.save();
            canvas.translate(0, landscape ? dp(1) : dp(27));
            drawText(canvas, objectiveText().replace("目标：", ""), center, top + dp(56), 14,
                    Color.rgb(111, 224, 190), Paint.Align.CENTER, true);

            canvas.restore();
            float statsTop = top + (landscape ? dp(72) : dp(103));
            float third = (rightEdge - leftEdge) / 3f;
            if (hudSkin != null) {
                for (int i = 0; i < 3; i++) {
                    float gap = dp(4);
                    RectF card = new RectF(leftEdge + third * i + gap, statsTop,
                            leftEdge + third * (i + 1) - gap, panelBottom - dp(7));
                    drawNineSlice(canvas, hudSkin, new Rect(70, 79, 772, 278), card, 48);
                    paint.setColor(Color.argb(178, 5, 11, 19));
                    canvas.drawRoundRect(card.left + dp(5), card.top + dp(5),
                            card.right - dp(5), card.bottom - dp(5), dp(7), dp(7), paint);
                }
            }
            int collected = initialTreasures - state.countEntities(Treasure.class);
            float statLabelY = top + (landscape ? dp(88) : dp(135));
            float statValueY = top + (landscape ? dp(106) : dp(171));
            drawStat(canvas, "步数", String.valueOf(state.getMoveCount()),
                    leftEdge + third * 0.5f, statLabelY, statValueY);
            drawStat(canvas, "宝物", collected + "/" + initialTreasures,
                    leftEdge + third * 1.5f, statLabelY, statValueY);
            drawStat(canvas, "钥匙", state.getPlayer().hasKey() ? "有" : "无",
                    leftEdge + third * 2.5f, statLabelY, statValueY);
        }

        private void drawNineSlice(Canvas canvas, Bitmap bitmap, Rect src, RectF dst, int corner) {
            int c = Math.min(corner, Math.min(src.width(), src.height()) / 2);
            float dc = Math.min(dp(24), Math.min(dst.width(), dst.height()) / 3f);
            int[] sx = {src.left, src.left + c, src.right - c, src.right};
            int[] sy = {src.top, src.top + c, src.bottom - c, src.bottom};
            float[] dx = {dst.left, dst.left + dc, dst.right - dc, dst.right};
            float[] dy = {dst.top, dst.top + dc, dst.bottom - dc, dst.bottom};
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    canvas.drawBitmap(bitmap,
                            new Rect(sx[col], sy[row], sx[col + 1], sy[row + 1]),
                            new RectF(dx[col], dy[row], dx[col + 1], dy[row + 1]), pixelPaint);
                }
            }
        }

        private void drawStat(Canvas canvas, String label, String value, float x,
                              float labelBaseline, float valueBaseline) {
            drawText(canvas, label, x, labelBaseline, 11,
                    Color.rgb(126, 145, 180), Paint.Align.CENTER, false);
            drawText(canvas, value, x, valueBaseline, 17,
                    Color.WHITE, Paint.Align.CENTER, true);
        }

        private String objectiveText() {
            return switch (level) {
                case 1 -> "目标：到达出口";
                case 2 -> "目标：收集全部宝物并到达出口";
                case 3 -> "目标：取得钥匙、打开门并到达出口";
                case 4 -> "目标：推动巨石压住全部机关";
                default -> "目标：完成宝物、机关与出口的终极试炼";
            };
        }

        private void drawBoard(Canvas canvas, float bx, float by, float cell) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(110, 0, 0, 0));
            canvas.drawRoundRect(bx - dp(9), by - dp(5), bx + cell * state.getWidth() + dp(9),
                    by + cell * state.getHeight() + dp(13), dp(10), dp(10), paint);
            if (hudSkin != null) {
                paint.setColor(Color.rgb(24, 32, 47));
                canvas.drawRect(bx - dp(3), by - dp(3),
                        bx + cell * state.getWidth() + dp(3),
                        by + cell * state.getHeight() + dp(3), paint);
            } else {
                paint.setColor(Color.rgb(3, 5, 10));
                canvas.drawRoundRect(bx - dp(6), by - dp(6), bx + cell * state.getWidth() + dp(6),
                        by + cell * state.getHeight() + dp(6), dp(8), dp(8), paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2));
                paint.setColor(Color.rgb(71, 80, 104));
                canvas.drawRoundRect(bx - dp(6), by - dp(6), bx + cell * state.getWidth() + dp(6),
                        by + cell * state.getHeight() + dp(6), dp(8), dp(8), paint);
            }
            paint.setStyle(Paint.Style.FILL);
            Position player = state.getPlayer().getPosition();
            for (int y = 0; y < state.getHeight(); y++) {
                for (int x = 0; x < state.getWidth(); x++) {
                    RectF dst = new RectF(bx + x * cell, by + y * cell,
                            bx + (x + 1) * cell, by + (y + 1) * cell);
                    drawFloor(canvas, dst, x, y);
                    Position p = new Position(x, y);
                    List<Entity> entities = new ArrayList<>(state.entitiesAt(p));
                    entities.sort(Comparator.comparingInt(this::layer));
                    for (Entity entity : entities) drawEntity(canvas, entity, dst);
                    if (player.equals(p)) drawEntity(canvas, state.getPlayer(), dst);
                }
            }
        }

        private void drawFloor(Canvas canvas, RectF dst, int x, int y) {
            if (sprites != null) {
                // The source cell contains a wide transparent gutter. Crop to its real
                // painted bounds before tiling so corridors form one continuous surface.
                drawCroppedTile(canvas, 2, 2, dst, 49, 24, 302, 293);
            } else {
                paint.setColor(((x + y) & 1) == 0 ? Color.rgb(40, 48, 62) : Color.rgb(46, 55, 70));
                canvas.drawRect(dst, paint);
            }
        }

        private void drawEntity(Canvas canvas, Entity entity, RectF cell) {
            int col = 0, row = 0;
            float inset = 0.09f;
            if (entity instanceof Wall) { col = 1; row = 0; inset = 0f; }
            else if (entity instanceof Exit) { col = 2; row = 0; }
            else if (entity instanceof Treasure) { col = 3; row = 0; }
            else if (entity instanceof Key) { col = 0; row = 1; }
            else if (entity instanceof Door door) { col = door.isOpen() ? 2 : 1; row = 1; }
            else if (entity instanceof Boulder) { col = 3; row = 1; }
            else if (entity instanceof FloorSwitch floorSwitch) {
                col = state.isSwitchCovered(floorSwitch.getPosition()) ? 1 : 0; row = 2;
            } else if (entity instanceof Player) { col = 0; row = 0; }
            if (sprites != null) {
                if (entity instanceof Wall) {
                    // Wall art also has a large transparent gutter. Filling the cell with
                    // the opaque art joins adjacent wall runs across all level files.
                    drawCroppedTile(canvas, col, row, cell, 42, 55, 310, 337);
                    return;
                }
                drawSprite(canvas, col, row, cell, inset);
                return;
            }
            float radius = Math.min(cell.width(), cell.height()) * 0.32f;
            paint.setColor(entity instanceof Player ? Color.rgb(67, 142, 220)
                    : entity instanceof Wall ? Color.rgb(72, 82, 105)
                    : entity instanceof Exit ? Color.rgb(64, 190, 112)
                    : entity instanceof Treasure || entity instanceof Key ? GOLD
                    : entity instanceof FloorSwitch ? Color.rgb(198, 70, 64)
                    : Color.rgb(130, 134, 145));
            canvas.drawCircle(cell.centerX(), cell.centerY(), radius, paint);
        }

        private void drawSprite(Canvas canvas, int col, int row, RectF cell, float insetFraction) {
            Rect src = new Rect(col * SHEET_CELL, row * SHEET_CELL,
                    (col + 1) * SHEET_CELL, (row + 1) * SHEET_CELL);
            float inset = Math.min(cell.width(), cell.height()) * insetFraction;
            RectF dst = new RectF(cell.left + inset, cell.top + inset,
                    cell.right - inset, cell.bottom - inset);
            canvas.drawBitmap(sprites, src, dst, pixelPaint);
        }

        private void drawCroppedTile(Canvas canvas, int col, int row, RectF dst,
                                     int cropLeft, int cropTop, int cropRight, int cropBottom) {
            Rect src = new Rect(col * SHEET_CELL + cropLeft, row * SHEET_CELL + cropTop,
                    col * SHEET_CELL + cropRight, row * SHEET_CELL + cropBottom);
            canvas.drawBitmap(sprites, src, dst, pixelPaint);
        }

        private int layer(Entity entity) {
            if (entity instanceof FloorSwitch) return 0;
            if (entity instanceof Exit) return 1;
            if (entity instanceof Treasure) return 2;
            if (entity instanceof Key) return 3;
            if (entity instanceof Door) return 4;
            if (entity instanceof Boulder) return 5;
            if (entity instanceof Wall) return 6;
            return 7;
        }

        private void drawControls(Canvas canvas, float x, float y, float width, float height) {
            if (uiSkin != null && getWidth() <= getHeight()) {
                drawSkinnedControls(canvas, x, y, width, height);
                return;
            }
            skinnedControlsActive = false;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(238, 14, 23, 39));
            canvas.drawRoundRect(x, y, x + width, y + height, dp(18), dp(18), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.rgb(55, 72, 104));
            canvas.drawRoundRect(x, y, x + width, y + height, dp(18), dp(18), paint);
            paint.setStyle(Paint.Style.FILL);

            float size = Math.min(width / 4.2f, height / 2.2f);
            size = Math.max(dp(44), Math.min(size, dp(70)));
            float cx = x + width / 2f;
            float cy = y + Math.min(height * 0.52f, dp(174));
            up.set(cx - size / 2, cy - size * 1.05f, cx + size / 2, cy - size * 0.05f);
            down.set(cx - size / 2, cy + size * 0.05f, cx + size / 2, cy + size * 1.05f);
            left.set(cx - size * 1.55f, cy - size / 2, cx - size * 0.55f, cy + size / 2);
            right.set(cx + size * 0.55f, cy - size / 2, cx + size * 1.55f, cy + size / 2);
            drawTouchButton(canvas, up, "▲", true);
            drawTouchButton(canvas, down, "▼", true);
            drawTouchButton(canvas, left, "◀", true);
            drawTouchButton(canvas, right, "▶", true);
            if (width > dp(260)) {
                float sideWidth = Math.min(dp(82), (width - size * 3.25f) / 2f);
                float sideHeight = Math.min(dp(76), height * 0.54f);
                restart.set(x + dp(10), cy - sideHeight / 2f,
                        x + dp(10) + sideWidth, cy + sideHeight / 2f);
                levels.set(x + width - dp(10) - sideWidth, cy - sideHeight / 2f,
                        x + width - dp(10), cy + sideHeight / 2f);
                menu.setEmpty();
                drawTouchButton(canvas, restart, "重开", false);
                drawTouchButton(canvas, levels, "选关", false);
            }
        }

        private void drawSkinnedControls(Canvas canvas, float x, float y, float width, float height) {
            skinnedControlsActive = true;
            Rect source = new Rect(0, 0, uiSkin.getWidth(), uiSkin.getHeight());
            float scale = Math.min(width / source.width(), height / source.height());
            float drawWidth = source.width() * scale;
            float drawHeight = source.height() * scale;
            float drawLeft = x + (width - drawWidth) / 2f;
            // Keep the console attached to the board instead of vertically centering it
            // inside surplus screen space and leaving a large empty band between them.
            float drawTop = y;
            RectF destination = new RectF(drawLeft, drawTop,
                    drawLeft + drawWidth, drawTop + drawHeight);
            Paint skinPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(7, 12, 21));
            canvas.drawRect(x, y, x + width, y + height, paint);
            canvas.drawBitmap(uiSkin, source, destination, skinPaint);

            up.set(drawLeft + uiSkin.getWidth() * .36f * scale, drawTop + uiSkin.getHeight() * .06f * scale,
                    drawLeft + uiSkin.getWidth() * .64f * scale, drawTop + uiSkin.getHeight() * .40f * scale);
            left.set(drawLeft + uiSkin.getWidth() * .245f * scale, drawTop + uiSkin.getHeight() * .30f * scale,
                    drawLeft + uiSkin.getWidth() * .495f * scale, drawTop + uiSkin.getHeight() * .64f * scale);
            right.set(drawLeft + uiSkin.getWidth() * .505f * scale, drawTop + uiSkin.getHeight() * .30f * scale,
                    drawLeft + uiSkin.getWidth() * .755f * scale, drawTop + uiSkin.getHeight() * .64f * scale);
            down.set(drawLeft + uiSkin.getWidth() * .36f * scale, drawTop + uiSkin.getHeight() * .55f * scale,
                    drawLeft + uiSkin.getWidth() * .64f * scale, drawTop + uiSkin.getHeight() * .92f * scale);
            restart.set(drawLeft + uiSkin.getWidth() * .03f * scale, drawTop + uiSkin.getHeight() * .34f * scale,
                    drawLeft + uiSkin.getWidth() * .24f * scale, drawTop + uiSkin.getHeight() * .68f * scale);
            levels.set(drawLeft + uiSkin.getWidth() * .76f * scale, drawTop + uiSkin.getHeight() * .34f * scale,
                    drawLeft + uiSkin.getWidth() * .97f * scale, drawTop + uiSkin.getHeight() * .68f * scale);
            menu.setEmpty();

            // Visual feedback follows the illuminated inner faces; touch rectangles above stay
            // slightly larger so the controls remain forgiving on small and high-density screens.
            setSkinArea(upVisual, drawLeft, drawTop, scale, .400f, .155f, .600f, .395f);
            setSkinArea(leftVisual, drawLeft, drawTop, scale, .250f, .335f, .460f, .600f);
            setSkinArea(rightVisual, drawLeft, drawTop, scale, .540f, .335f, .750f, .600f);
            setSkinArea(downVisual, drawLeft, drawTop, scale, .395f, .555f, .605f, .805f);
            setSkinArea(restartVisual, drawLeft, drawTop, scale, .070f, .385f, .215f, .575f);
            setSkinArea(levelsVisual, drawLeft, drawTop, scale, .785f, .385f, .930f, .575f);

            setSkinPolygon(upShape, drawLeft, drawTop, scale,
                    .500f,.145f, .615f,.270f, .615f,.305f, .500f,.425f,
                    .385f,.305f, .385f,.270f);
            setSkinPolygon(leftShape, drawLeft, drawTop, scale,
                    .350f,.315f, .500f,.445f, .500f,.490f, .350f,.625f,
                    .225f,.490f, .225f,.445f);
            setSkinPolygon(rightShape, drawLeft, drawTop, scale,
                    .650f,.315f, .775f,.445f, .775f,.490f, .650f,.625f,
                    .500f,.490f, .500f,.445f);
            setSkinPolygon(downShape, drawLeft, drawTop, scale,
                    .500f,.535f, .630f,.655f, .630f,.690f, .500f,.820f,
                    .370f,.690f, .370f,.655f);
            setSkinPolygon(restartShape, drawLeft, drawTop, scale,
                    .075f,.385f, .200f,.385f, .215f,.405f, .215f,.560f,
                    .200f,.580f, .075f,.580f, .060f,.560f, .060f,.405f);
            setSkinPolygon(levelsShape, drawLeft, drawTop, scale,
                    .800f,.385f, .925f,.385f, .940f,.405f, .940f,.560f,
                    .925f,.580f, .800f,.580f, .785f,.560f, .785f,.405f);

            if (pressedTarget != null) {
                Path pressedShape = shapeFor(pressedTarget);
                if (pressedShape != null) {
                    drawShapeFeedback(canvas, pressedShape);
                    if (pressedTarget == up || pressedTarget == down
                            || pressedTarget == left || pressedTarget == right) {
                        redrawCenterCap(canvas, destination);
                    }
                }
            }
        }

        private void redrawCenterCap(Canvas canvas, RectF destination) {
            Rect sourceCap = new Rect(570, 405, 830, 665);
            float sx = destination.width() / uiSkin.getWidth();
            float sy = destination.height() / uiSkin.getHeight();
            RectF targetCap = new RectF(destination.left + sourceCap.left * sx,
                    destination.top + sourceCap.top * sy,
                    destination.left + sourceCap.right * sx,
                    destination.top + sourceCap.bottom * sy);
            canvas.drawBitmap(uiSkin, sourceCap, targetCap, pixelPaint);
        }

        private void setSkinPolygon(Path target, float drawLeft, float drawTop, float scale,
                                    float... points) {
            target.reset();
            for (int i = 0; i < points.length; i += 2) {
                float px = drawLeft + uiSkin.getWidth() * points[i] * scale;
                float py = drawTop + uiSkin.getHeight() * points[i + 1] * scale;
                if (i == 0) target.moveTo(px, py); else target.lineTo(px, py);
            }
            target.close();
        }

        private Path shapeFor(RectF target) {
            if (target == up) return upShape;
            if (target == down) return downShape;
            if (target == left) return leftShape;
            if (target == right) return rightShape;
            if (target == restart) return restartShape;
            if (target == levels) return levelsShape;
            return null;
        }

        private void drawShapeFeedback(Canvas canvas, Path shape) {
            // The same Path instance is used by targetAt() for hit-testing and here for
            // rendering, guaranteeing identical coordinates and boundaries.
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(150, 76, 226, 238));
            canvas.drawPath(shape, paint);
        }

        private void setSkinArea(RectF target, float drawLeft, float drawTop, float scale,
                                 float leftN, float topN, float rightN, float bottomN) {
            target.set(drawLeft + uiSkin.getWidth() * leftN * scale,
                    drawTop + uiSkin.getHeight() * topN * scale,
                    drawLeft + uiSkin.getWidth() * rightN * scale,
                    drawTop + uiSkin.getHeight() * bottomN * scale);
        }

        private RectF visualFor(RectF touchTarget) {
            if (touchTarget == up) return upVisual;
            if (touchTarget == down) return downVisual;
            if (touchTarget == left) return leftVisual;
            if (touchTarget == right) return rightVisual;
            if (touchTarget == restart) return restartVisual;
            if (touchTarget == levels) return levelsVisual;
            return touchTarget;
        }

        private void drawDiamondFeedback(Canvas canvas, RectF rect) {
            float leftEdge = rect.left;
            float rightEdge = rect.right;
            float topEdge = rect.top;
            float bottomEdge = rect.bottom;
            Path diamond = new Path();
            diamond.moveTo(rect.centerX(), topEdge);
            diamond.lineTo(rightEdge, rect.centerY());
            diamond.lineTo(rect.centerX(), bottomEdge);
            diamond.lineTo(leftEdge, rect.centerY());
            diamond.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(76, 61, 226, 238));
            canvas.drawPath(diamond, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.argb(235, 93, 239, 243));
            canvas.drawPath(diamond, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawButtonFeedback(Canvas canvas, RectF rect) {
            RectF highlight = new RectF(rect);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(70, 72, 218, 232));
            canvas.drawRoundRect(highlight, dp(8), dp(8), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.argb(225, 111, 237, 242));
            canvas.drawRoundRect(highlight, dp(8), dp(8), paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawNavigation(Canvas canvas, float x, float y, float width, float height) {
            float gap = dp(7);
            float each = (width - gap * 2) / 3f;
            restart.set(x, y, x + each, y + height);
            levels.set(x + each + gap, y, x + each * 2 + gap, y + height);
            menu.set(x + each * 2 + gap * 2, y, x + width, y + height);
            drawTouchButton(canvas, restart, "重开", false);
            drawTouchButton(canvas, levels, "选关", false);
            drawTouchButton(canvas, menu, "菜单", false);
        }

        private void drawTouchButton(Canvas canvas, RectF rect, String label, boolean direction) {
            paint.setStyle(Paint.Style.FILL);
            boolean pressed = pressedTarget == rect;
            paint.setColor(pressed
                    ? (direction ? Color.rgb(42, 135, 194) : Color.rgb(68, 93, 122))
                    : (direction ? Color.rgb(29, 63, 112) : Color.rgb(31, 40, 61)));
            canvas.drawRoundRect(rect, dp(10), dp(10), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(direction ? Color.rgb(83, 143, 222) : Color.rgb(89, 101, 132));
            canvas.drawRoundRect(rect, dp(10), dp(10), paint);
            paint.setStyle(Paint.Style.FILL);
            drawText(canvas, label, rect.centerX(), rect.centerY() + dp(direction ? 8 : 5),
                    direction ? 22 : 13,
                    pressed ? Color.rgb(168, 255, 255) : Color.WHITE,
                    Paint.Align.CENTER, true);
        }

        private void drawText(Canvas canvas, String text, float x, float baseline, float sp,
                              int color, Paint.Align align, boolean bold) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
            paint.setTextSize(sp * getResources().getDisplayMetrics().scaledDensity);
            paint.setTextAlign(align);
            paint.setTypeface(bold ? android.graphics.Typeface.DEFAULT_BOLD
                    : android.graphics.Typeface.DEFAULT);
            canvas.drawText(text, x, baseline, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                touchX = event.getX();
                touchY = event.getY();
                pressedTarget = targetAt(touchX, touchY);
                if (pressedTarget != null && pressedTarget != up && pressedTarget != down
                        && pressedTarget != left && pressedTarget != right) {
                    feedback.click(this);
                }
                invalidate();
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                pressedTarget = null;
                invalidate();
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                RectF hovered = targetAt(event.getX(), event.getY());
                if (hovered != pressedTarget) {
                    pressedTarget = hovered;
                    invalidate();
                }
                return true;
            }
            if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;
            float x = event.getX(), y = event.getY();
            float dx = x - touchX, dy = y - touchY;
            float swipe = dp(32);
            if (Math.hypot(dx, dy) >= swipe) {
                pressedTarget = null;
                invalidate();
                move(Math.abs(dx) > Math.abs(dy)
                        ? (dx > 0 ? Direction.RIGHT : Direction.LEFT)
                        : (dy > 0 ? Direction.DOWN : Direction.UP));
                return true;
            }
            performClick();
            RectF released = pressedTarget;
            RectF releasedAt = targetAt(x, y);
            if (released == up && releasedAt == up) move(Direction.UP);
            else if (released == down && releasedAt == down) move(Direction.DOWN);
            else if (released == left && releasedAt == left) move(Direction.LEFT);
            else if (released == right && releasedAt == right) move(Direction.RIGHT);
            else if (released == restart && releasedAt == restart) restartLevel();
            else if (released == levels && releasedAt == levels) showLevelSelect();
            else if (released == menu && releasedAt == menu) showMainMenu();
            pressedTarget = null;
            invalidate();
            return true;
        }

        private RectF targetAt(float x, float y) {
            if (skinnedControlsActive) {
                if (pathContains(upShape, x, y)) return up;
                if (pathContains(downShape, x, y)) return down;
                if (pathContains(leftShape, x, y)) return left;
                if (pathContains(rightShape, x, y)) return right;
                if (pathContains(restartShape, x, y)) return restart;
                if (pathContains(levelsShape, x, y)) return levels;
                return null;
            }
            if (up.contains(x, y)) return up;
            if (down.contains(x, y)) return down;
            if (left.contains(x, y)) return left;
            if (right.contains(x, y)) return right;
            if (restart.contains(x, y)) return restart;
            if (levels.contains(x, y)) return levels;
            if (menu.contains(x, y)) return menu;
            return null;
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        private void move(Direction direction) {
            MoveResult result = engine.move(direction);
            feedback.move(this, result);
            invalidate();
        }
    }
}
