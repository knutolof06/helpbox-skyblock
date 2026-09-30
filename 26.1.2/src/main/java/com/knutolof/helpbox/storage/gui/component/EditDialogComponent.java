package com.knutolof.helpbox.storage.gui.component;

import com.daqem.uilib.api.component.IComponent;
import com.daqem.uilib.api.widget.IWidget;
import com.daqem.uilib.gui.component.AbstractComponent;
import com.daqem.uilib.gui.component.text.TextComponent;
import com.daqem.uilib.gui.widget.ButtonWidget;
import com.daqem.uilib.gui.widget.EditBoxWidget;
import com.daqem.uilib.util.ValidationErrors;
import com.knutolof.helpbox.storage.storage.StorageKey;
import com.knutolof.helpbox.storage.storage.StorageNames;
import com.knutolof.helpbox.storage.storage.StorageOrder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;

/**
 * A modal dialog for renaming a storage page. Rendered on top of the storage
 * overlay: a dimming scrim covers the whole screen, with a centered panel
 * holding a title, a single-line {@link EditBoxWidget}, and Save / Cancel
 * buttons (plus Reset when a custom name already exists).
 *
 * <p>This component is purely visual + input; the owning screen supplies the
 * callbacks that actually persist, clear, or discard the name.</p>
 */
public class EditDialogComponent extends AbstractComponent {

    private static final int PANEL_WIDTH = 200;
    private static final int PANEL_HEIGHT = 132;

    // Scrim + panel colours (ARGB). The panel is a simple filled rect with a
    // lighter border so the dialog reads clearly over any background type.
    private static final int SCRIM_COLOR = 0xC0000000;
    private static final int PANEL_FILL = 0xF0202020;
    private static final int PANEL_BORDER = 0xFF000000;
    private static final int PANEL_HILIGHT = 0xFF555555;

    private final StorageKey key;
    private final EditBoxWidget nameBox;
    private final EditBoxWidget positionBox;
    private final int panelX;
    private final int panelY;
    private @Nullable TextComponent takenHint;

    /**
     * @param screenWidth  full screen width  (scrim spans this)
     * @param screenHeight full screen height (scrim spans this)
     * @param font         client font
     * @param key          the page being renamed
     * @param onSave       accepts the entered name (already the raw box value)
     * @param onCancel     discards without changes
     * @param onReset      clears the custom name; null hides the Reset button
     */
    public EditDialogComponent(int screenWidth, int screenHeight, Font font,
                               StorageKey key,
                               int defaultPosition, IntFunction<String> positionTakenBy,
                               BiConsumer<String, String> onSave,
                               Runnable onCancel,
                               Runnable onReset) {
        super(0, 0, screenWidth, screenHeight);
        this.key = key;

        this.panelX = (screenWidth - PANEL_WIDTH) / 2;
        this.panelY = (screenHeight - PANEL_HEIGHT) / 2;

        boolean hasCustom = StorageNames.getInstance().has(key) || StorageOrder.getInstance().has(key);

        // Title
        String defaultName = key.displayName();
        TextComponent title = new TextComponent(
                panelX + 8, panelY + 8,
                Component.translatable("enhanced_storage.ui.edit_title", defaultName),
                0xFFFFFFFF);
        title.setDrawShadow(true);
        this.addComponent(title);

        TextComponent nameLabel = new TextComponent(panelX + 8, panelY + 22,
                Component.translatable("enhanced_storage.ui.name"), 0xFFAAAAAA);
        nameLabel.setDrawShadow(true);
        this.addComponent(nameLabel);

        // Edit box, prefilled with the current custom name (if any).
        int boxX = panelX + 8;
        int boxWidth = PANEL_WIDTH - 16;
        this.nameBox = new EditBoxWidget(font, boxX, panelY + 32, boxWidth, 16,
                Component.translatable("enhanced_storage.ui.page_name"));
        this.nameBox.setValue(StorageNames.getInstance().get(key).orElse(""));
        this.addWidget(this.nameBox);

        // Position row
        Component posLabelComp = defaultPosition > 0
                ? Component.translatable("enhanced_storage.ui.pos_default", defaultPosition)
                : Component.translatable("enhanced_storage.ui.pos_blank");
        TextComponent posLabel = new TextComponent(panelX + 8, panelY + 54,
                posLabelComp, 0xFFAAAAAA);
        posLabel.setDrawShadow(true);
        this.addComponent(posLabel);

        this.positionBox = new EditBoxWidget(font, boxX, panelY + 64, boxWidth, 16, Component.translatable("enhanced_storage.ui.position")) {
            @Override
            public List<Component> validateInput(String input) {
                List<Component> errors = new ArrayList<>();
                if (input.isEmpty()) return errors;
                try {
                    int pos = Integer.parseInt(input);
                    if (pos < -999) errors.add(ValidationErrors.minValue(-999));
                    if (pos > 9999) errors.add(ValidationErrors.maxValue(9999));
                } catch (NumberFormatException e) {
                    errors.add(ValidationErrors.invalidNumber());
                }
                return errors;
            }
        };
        this.positionBox.setValue(StorageOrder.getInstance().get(key)
                .map(String::valueOf).orElse(""));
        this.addWidget(this.positionBox);

        // Add buttons, the reset button only appears when a custom name is set.
        int btnY = panelY + PANEL_HEIGHT - 26;
        int gap = 6;
        int innerWidth = PANEL_WIDTH - 16;

        Runnable doSave = () -> onSave.accept(nameBox.getValue(), positionBox.getValue());

        final ButtonWidget saveBtn;
        if (hasCustom && onReset != null) {
            int btnWidth = (innerWidth - gap * 2) / 3;
            saveBtn = new ButtonWidget(panelX + 8, btnY, btnWidth, 20,
                    Component.translatable("enhanced_storage.ui.save"), b -> doSave.run());
            ButtonWidget resetBtn = new ButtonWidget(panelX + 8 + btnWidth + gap, btnY, btnWidth, 20,
                    Component.translatable("enhanced_storage.ui.reset"), b -> onReset.run());
            ButtonWidget cancelBtn = new ButtonWidget(panelX + 8 + (btnWidth + gap) * 2, btnY, btnWidth, 20,
                    Component.translatable("enhanced_storage.ui.cancel"), b -> onCancel.run());
            this.addWidget(saveBtn);
            this.addWidget(resetBtn);
            this.addWidget(cancelBtn);
        } else {
            int btnWidth = (innerWidth - gap) / 2;
            saveBtn = new ButtonWidget(panelX + 8, btnY, btnWidth, 20,
                    Component.translatable("enhanced_storage.ui.save"), b -> doSave.run());
            ButtonWidget cancelBtn = new ButtonWidget(panelX + 8 + btnWidth + gap, btnY, btnWidth, 20,
                    Component.translatable("enhanced_storage.ui.cancel"), b -> onCancel.run());
            this.addWidget(saveBtn);
            this.addWidget(cancelBtn);
        }

        this.positionBox.setResponder(text -> {
            saveBtn.active = positionBox.validateInput(text).isEmpty();
            updateTakenHint(text, positionTakenBy);
        });
    }

    private void updateTakenHint(String text, IntFunction<String> positionTakenBy) {
        String takenByName = null;
        try {
            if (!text.isEmpty()) takenByName = positionTakenBy.apply(Integer.parseInt(text));
        } catch (NumberFormatException ignored) {
            // invalid number: validation already shows red, no hint needed
        }

        if (takenByName != null) {
            if (takenHint == null) {
                takenHint = new TextComponent(panelX + 8, panelY + 84,
                        Component.translatable("enhanced_storage.ui.pos_used_by", takenByName), 0xFFFFAA00);
                takenHint.setDrawShadow(true);
                this.addComponent(takenHint);
            } else {
                takenHint.setText(Component.translatable("enhanced_storage.ui.pos_used_by", takenByName));
            }
        } else if (takenHint != null) {
            this.getComponents().remove(takenHint);
            takenHint = null;
        }
    }

    public StorageKey getKey() {
        return key;
    }

    public EditBoxWidget getNameBox() {
        return nameBox;
    }

    public EditBoxWidget getPositionBox() {
        return positionBox;
    }

    public EditBoxWidget getFocusedBox() {
        return positionBox.isFocused() ? positionBox : nameBox;
    }

    /**
     * True if the point is inside the dialog panel (not just the scrim).
     */
    public boolean isOverPanel(double x, double y) {
        return x >= panelX && x < panelX + PANEL_WIDTH
                && y >= panelY && y < panelY + PANEL_HEIGHT;
    }

    // Render order: scrim + panel first (in extractRenderState), then child
    // widgets (edit box, buttons), then any child components (the title).
    @Override
    public void extractRenderStateBase(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                       float partialTick, int parentWidth, int parentHeight) {
        this.extractRenderState(guiGraphics, mouseX, mouseY, partialTick, parentWidth, parentHeight);

        for (IWidget widget : getWidgets()) {
            widget.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }

        for (IComponent component : getComponents()) {
            component.extractRenderStateBase(guiGraphics, mouseX, mouseY, partialTick, getWidth(), getHeight());
        }
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                   float partialTick, int parentWidth, int parentHeight) {
        // Full-screen dimming scrim.
        guiGraphics.fill(0, 0, getWidth(), getHeight(), SCRIM_COLOR);

        // Panel: border, subtle top/left highlight, then fill.
        guiGraphics.fill(panelX - 1, panelY - 1, panelX + PANEL_WIDTH + 1, panelY + PANEL_HEIGHT + 1, PANEL_BORDER);
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, PANEL_FILL);
        // 1px inner highlight along the top and left edges.
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + 1, PANEL_HILIGHT);
        guiGraphics.fill(panelX, panelY, panelX + 1, panelY + PANEL_HEIGHT, PANEL_HILIGHT);
    }
}