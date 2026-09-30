package com.knutolof.helpbox.console;

import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import com.knutolof.helpbox.ui.ModernUiRenderHelper;
import com.knutolof.helpbox.util.HelpBoxLang;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import com.mojang.blaze3d.platform.InputConstants;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ConsoleScreen extends Screen {

    private EditBox searchBox;
    private EditBox commandBox;
    private double scrollAmount = 0;

    // Scrollbar state
    private boolean isDraggingScrollbar = false;
    private double scrollbarDragStartY = 0;
    private double scrollbarDragStartScroll = 0;

    // Letter-by-letter text selection state
    private boolean isSelecting = false;
    private int selectionStartLine = -1;
    private int selectionStartChar = -1;
    private int selectionEndLine = -1;
    private int selectionEndChar = -1;

    // Click tracking for Clickable Events vs Drag Selection
    private double mouseDownX = 0;
    private double mouseDownY = 0;
    private double totalDragDistance = 0;
    private ClickableSpan pendingClickedSpan = null;

    // Popup state for Clickable Actions ("Git" Button)
    private ClickEvent activePopupClickEvent = null;
    private String activePopupSpanText = "";
    private Button gitButton;
    private Button copyCmdButton;
    private Button cancelPopupButton;

    private String notificationText = "";
    private long notificationTimeMs = 0;

    public record ClickableSpan(int startChar, int endChar, String text, ClickEvent clickEvent) {}

    public ConsoleScreen() {
        super(HelpBoxLang.tr("helpbox.ui.console_screen.title"));
    }

    private int getContentTop() {
        return 38;
    }

    private int getContentBottom() {
        return this.height - (activePopupClickEvent != null ? 120 : 38);
    }

    private int getContentLeft() {
        return 10;
    }

    private int getContentRight() {
        return this.width - 10;
    }

    private int getScrollbarWidth() {
        return 6;
    }

    private int getScrollbarX() {
        return getContentRight() - getScrollbarWidth() - 3;
    }

    private int getScrollbarY() {
        return getContentTop() + 2;
    }

    private int getScrollbarHeight() {
        return Math.max(10, getContentBottom() - getContentTop() - 4);
    }

    public int getMaxScroll() {
        int lineHeight = this.font.lineHeight + 4;
        List<ConsoleHistory.ConsoleEntry> filtered = getFilteredEntries();
        int totalHeight = filtered.size() * lineHeight + 8;
        int viewHeight = getContentBottom() - getContentTop();
        return Math.max(0, totalHeight - viewHeight);
    }

    private int getThumbHeight(int viewHeight, int totalHeight, int scrollbarHeight) {
        if (totalHeight <= 0) return scrollbarHeight;
        int th = (int) ((float) viewHeight / totalHeight * scrollbarHeight);
        return Math.max(16, Math.min(scrollbarHeight, th));
    }

    private int getThumbY(int maxScroll, int scrollbarY, int scrollbarHeight, int thumbHeight) {
        if (maxScroll <= 0) return scrollbarY;
        float ratio = (float) (scrollAmount / (double) maxScroll);
        ratio = Math.max(0.0f, Math.min(1.0f, ratio));
        int available = scrollbarHeight - thumbHeight;
        return scrollbarY + (int) (ratio * available);
    }

    public void scrollToBottom() {
        this.scrollAmount = getMaxScroll();
    }

    @Override
    protected void init() {
        super.init();

        int topY = 10;
        int searchWidth = 130;
        int searchX = 10;

        searchBox = new EditBox(this.font, searchX, topY, searchWidth, 20, HelpBoxLang.tr("helpbox.ui.console_screen.search_hint"));
        searchBox.setHint(HelpBoxLang.tr("helpbox.ui.console_screen.search_hint"));
        searchBox.setResponder(val -> scrollAmount = 0);
        this.addRenderableWidget(searchBox);

        int btnX = searchX + searchWidth + 6;

        this.addRenderableWidget(Button.builder(HelpBoxLang.tr("helpbox.ui.console_screen.copy_selected"), btn -> {
            copySelectedText();
        }).bounds(btnX, topY, 95, 20).build());

        btnX += 99;
        this.addRenderableWidget(Button.builder(HelpBoxLang.tr("helpbox.ui.console_screen.copy_all"), btn -> {
            copyAllText();
        }).bounds(btnX, topY, 90, 20).build());

        btnX += 94;
        this.addRenderableWidget(Button.builder(HelpBoxLang.tr("helpbox.ui.console_screen.bottom_line"), btn -> {
            scrollToBottom();
            showNotification(HelpBoxLang.str("helpbox.ui.console_screen.notif.scrolled_bottom"));
        }).bounds(btnX, topY, 82, 20).build());

        btnX += 86;
        this.addRenderableWidget(Button.builder(HelpBoxLang.tr("helpbox.ui.console_screen.clear"), btn -> {
            ConsoleHistory.clear();
            showNotification(HelpBoxLang.str("helpbox.ui.console_screen.notif.cleared"));
            scrollAmount = 0;
        }).bounds(btnX, topY, 58, 20).build());

        // Kapat butonu sağ köşeye yaslı
        this.addRenderableWidget(Button.builder(HelpBoxLang.tr("helpbox.ui.console_screen.close"), btn -> {
            this.onClose();
        }).bounds(this.width - 65, topY, 55, 20).build());

        // Command Input Box at bottom
        int bottomY = this.height - 30;
        commandBox = new EditBox(this.font, 10, bottomY, this.width - 20, 20, HelpBoxLang.tr("helpbox.ui.console_screen.cmd_hint"));
        commandBox.setHint(HelpBoxLang.tr("helpbox.ui.console_screen.cmd_hint"));
        this.addRenderableWidget(commandBox);

        // --- Floating Popup Action Buttons (Git / Kopyala / İptal) ---
        int popupWidth = 320;
        int popupX = (this.width - popupWidth) / 2;
        int popupY = this.height - 110;

        gitButton = Button.builder(HelpBoxLang.tr("helpbox.ui.console_screen.git_btn"), btn -> executeGitAction()).bounds(popupX + 15, popupY + 38, 90, 20).build();
        copyCmdButton = Button.builder(HelpBoxLang.tr("helpbox.ui.console_screen.copy_cmd"), btn -> copyActionCommand()).bounds(popupX + 115, popupY + 38, 100, 20).build();
        cancelPopupButton = Button.builder(HelpBoxLang.tr("helpbox.ui.console_screen.cancel"), btn -> closePopup()).bounds(popupX + 225, popupY + 38, 80, 20).build();

        gitButton.visible = false;
        copyCmdButton.visible = false;
        cancelPopupButton.visible = false;

        this.addRenderableWidget(gitButton);
        this.addRenderableWidget(copyCmdButton);
        this.addRenderableWidget(cancelPopupButton);

        // Konsol açıldığında otomatik olarak en son eklenen satıra git
        scrollToBottom();
    }

    private List<ConsoleHistory.ConsoleEntry> getFilteredEntries() {
        List<ConsoleHistory.ConsoleEntry> all = ConsoleHistory.getEntries();
        String query = searchBox != null ? searchBox.getValue().trim().toLowerCase() : "";
        if (query.isEmpty()) return all;

        List<ConsoleHistory.ConsoleEntry> filtered = new ArrayList<>();
        for (ConsoleHistory.ConsoleEntry entry : all) {
            if (entry.plainText().toLowerCase().contains(query) || entry.timeStr().contains(query)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }

    private List<ClickableSpan> getClickableSpans(ConsoleHistory.ConsoleEntry entry) {
        List<ClickableSpan> spans = new ArrayList<>();
        String timePrefix = "[" + entry.timeStr() + "] ";
        int prefixLen = timePrefix.length();

        StringBuilder fullText = new StringBuilder();

        entry.component().visit((style, text) -> {
            int start = prefixLen + fullText.length();
            fullText.append(text);
            int end = prefixLen + fullText.length();

            if (style != null && style.getClickEvent() != null) {
                spans.add(new ClickableSpan(start, end, text, style.getClickEvent()));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);

        return spans;
    }

    private int getCharIndexAtX(String text, int mouseX, int lineX) {
        int relX = mouseX - lineX;
        if (relX <= 0) return 0;

        int totalW = 0;
        for (int i = 0; i < text.length(); i++) {
            int charW = this.font.width(text.substring(i, i + 1));
            if (totalW + charW / 2 >= relX) {
                return i;
            }
            totalW += charW;
        }
        return text.length();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
        if (super.mouseClicked(event, handled)) return true;

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        mouseDownX = mouseX;
        mouseDownY = mouseY;
        totalDragDistance = 0;
        pendingClickedSpan = null;

        int sbX = getScrollbarX();
        int sbY = getScrollbarY();
        int sbW = getScrollbarWidth();
        int sbH = getScrollbarHeight();

        // 1. Scrollbar interaction
        if (mouseX >= sbX - 4 && mouseX <= sbX + sbW + 4 && mouseY >= sbY && mouseY <= sbY + sbH) {
            int maxScroll = getMaxScroll();
            if (maxScroll > 0) {
                int lineHeight = this.font.lineHeight + 4;
                List<ConsoleHistory.ConsoleEntry> filtered = getFilteredEntries();
                int totalHeight = filtered.size() * lineHeight + 8;
                int viewHeight = getContentBottom() - getContentTop();
                int thumbHeight = getThumbHeight(viewHeight, totalHeight, sbH);
                int thumbY = getThumbY(maxScroll, sbY, sbH, thumbHeight);

                if (mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                    // Clicked on thumb -> start dragging
                    isDraggingScrollbar = true;
                    scrollbarDragStartY = mouseY;
                    scrollbarDragStartScroll = scrollAmount;
                } else {
                    // Clicked on track above or below thumb -> jump scroll position directly
                    int available = sbH - thumbHeight;
                    if (available > 0) {
                        float clickRatio = (float) (mouseY - sbY - thumbHeight / 2) / available;
                        scrollAmount = Math.max(0, Math.min(maxScroll, clickRatio * maxScroll));
                        isDraggingScrollbar = true;
                        scrollbarDragStartY = mouseY;
                        scrollbarDragStartScroll = scrollAmount;
                    }
                }
                return true;
            }
        }

        // 2. Text Selection & Clickable Spans
        int contentTop = getContentTop();
        int contentBottom = getContentBottom();

        if (mouseY >= contentTop && mouseY <= contentBottom && mouseX < sbX - 4) {
            List<ConsoleHistory.ConsoleEntry> filtered = getFilteredEntries();
            int lineHeight = this.font.lineHeight + 4;
            int clickedLine = (int) ((mouseY - contentTop + scrollAmount) / lineHeight);

            if (clickedLine >= 0 && clickedLine < filtered.size()) {
                ConsoleHistory.ConsoleEntry entry = filtered.get(clickedLine);
                String fullLine = "[" + entry.timeStr() + "] " + entry.plainText();
                int lineX = 15;
                int charIdx = getCharIndexAtX(fullLine, mouseX, lineX);

                // Check if user clicked directly on a ClickableSpan
                List<ClickableSpan> spans = getClickableSpans(entry);
                for (ClickableSpan span : spans) {
                    if (charIdx >= span.startChar() && charIdx <= span.endChar()) {
                        pendingClickedSpan = span;
                        break;
                    }
                }

                isSelecting = true;
                selectionStartLine = clickedLine;
                selectionStartChar = charIdx;
                selectionEndLine = clickedLine;
                selectionEndChar = charIdx;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        totalDragDistance += Math.abs(deltaX) + Math.abs(deltaY);
        if (totalDragDistance > 4) {
            pendingClickedSpan = null; // User is dragging, not clicking span
        }

        if (isDraggingScrollbar) {
            int maxScroll = getMaxScroll();
            if (maxScroll > 0) {
                int lineHeight = this.font.lineHeight + 4;
                List<ConsoleHistory.ConsoleEntry> filtered = getFilteredEntries();
                int totalHeight = filtered.size() * lineHeight + 8;
                int viewHeight = getContentBottom() - getContentTop();
                int sbH = getScrollbarHeight();
                int thumbHeight = getThumbHeight(viewHeight, totalHeight, sbH);
                int available = sbH - thumbHeight;
                if (available > 0) {
                    double deltaYDrag = event.y() - scrollbarDragStartY;
                    double scrollDelta = (deltaYDrag / (double) available) * maxScroll;
                    scrollAmount = Math.max(0, Math.min(maxScroll, scrollbarDragStartScroll + scrollDelta));
                }
            }
            return true;
        }

        if (isSelecting) {
            int mouseX = (int) event.x();
            int mouseY = (int) event.y();
            int contentTop = getContentTop();
            int lineHeight = this.font.lineHeight + 4;

            List<ConsoleHistory.ConsoleEntry> filtered = getFilteredEntries();
            int currentLine = (int) ((mouseY - contentTop + scrollAmount) / lineHeight);
            currentLine = Math.max(0, Math.min(filtered.size() - 1, currentLine));

            if (currentLine >= 0 && currentLine < filtered.size()) {
                ConsoleHistory.ConsoleEntry entry = filtered.get(currentLine);
                String fullLine = "[" + entry.timeStr() + "] " + entry.plainText();
                int lineX = 15;
                int charIdx = getCharIndexAtX(fullLine, mouseX, lineX);

                selectionEndLine = currentLine;
                selectionEndChar = charIdx;
            }
            return true;
        }
        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (isDraggingScrollbar) {
            isDraggingScrollbar = false;
        }
        if (totalDragDistance <= 4 && pendingClickedSpan != null) {
            // Open the "Git" action popup!
            openActionPopup(pendingClickedSpan);
            pendingClickedSpan = null;
        }
        if (isSelecting) {
            isSelecting = false;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = getMaxScroll();
        int lineHeight = this.font.lineHeight + 4;
        scrollAmount = Math.max(0, Math.min(maxScroll, scrollAmount - verticalAmount * lineHeight * 2));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();

        if (keyCode == InputConstants.KEY_ESCAPE && activePopupClickEvent != null) {
            closePopup();
            return true;
        }

        // Enter to send command/message
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            if (commandBox != null && commandBox.isFocused()) {
                String cmd = commandBox.getValue().trim();
                if (!cmd.isEmpty()) {
                    sendCommand(cmd);
                    commandBox.setValue("");
                }
                return true;
            }
        }

        // Ctrl+C to copy selected text
        if (keyCode == InputConstants.KEY_C) {
            boolean ctrlDown = InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)
                    || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
            if (ctrlDown) {
                copySelectedText();
                return true;
            }
        }

        // Close console with the same keybind
        if (com.knutolof.helpbox.config.HelpBoxConfig.openConsole != null
                && com.knutolof.helpbox.config.HelpBoxConfig.openConsole.matches(event)) {
            this.onClose();
            return true;
        }

        return super.keyPressed(event);
    }

    private void openActionPopup(ClickableSpan span) {
        activePopupClickEvent = span.clickEvent();
        activePopupSpanText = span.text();

        gitButton.visible = true;
        copyCmdButton.visible = true;
        cancelPopupButton.visible = true;

        showNotification(HelpBoxLang.str("helpbox.ui.console_screen.notif.action_selected", span.text()));
    }

    private void closePopup() {
        activePopupClickEvent = null;
        activePopupSpanText = "";

        gitButton.visible = false;
        copyCmdButton.visible = false;
        cancelPopupButton.visible = false;
    }

    private static String getClickEventValue(ClickEvent event) {
        if (event == null) return "";
        if (event instanceof ClickEvent.RunCommand runCmd) {
            return runCmd.command();
        } else if (event instanceof ClickEvent.OpenUrl openUrl) {
            return openUrl.uri().toString();
        } else if (event instanceof ClickEvent.SuggestCommand suggestCmd) {
            return suggestCmd.command();
        } else if (event instanceof ClickEvent.CopyToClipboard copyClip) {
            return copyClip.value();
        }
        return event.toString();
    }

    private void executeGitAction() {
        if (activePopupClickEvent == null) return;
        ClickEvent event = activePopupClickEvent;
        closePopup();

        String val = getClickEventValue(event);

        if (event instanceof ClickEvent.RunCommand) {
            sendCommand(val);
        } else if (event instanceof ClickEvent.SuggestCommand) {
            if (commandBox != null) {
                commandBox.setValue(val);
                commandBox.setFocused(true);
            }
            showNotification(HelpBoxLang.str("helpbox.ui.console_screen.notif.typed_box", val));
        } else if (event instanceof ClickEvent.OpenUrl) {
            try {
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                    java.awt.Desktop.getDesktop().browse(new URI(val));
                } else {
                    Minecraft.getInstance().keyboardHandler.setClipboard(val);
                }
                showNotification(HelpBoxLang.str("helpbox.ui.console_screen.notif.link_opened", val));
            } catch (Exception e) {
                Minecraft.getInstance().keyboardHandler.setClipboard(val);
                showNotification(HelpBoxLang.str("helpbox.ui.console_screen.notif.link_copied", val));
            }
        } else if (event instanceof ClickEvent.CopyToClipboard) {
            Minecraft.getInstance().keyboardHandler.setClipboard(val);
            showNotification(HelpBoxLang.str("helpbox.ui.console_screen.notif.copied", val));
        } else {
            sendCommand(val);
        }
    }

    private void copyActionCommand() {
        if (activePopupClickEvent != null) {
            String val = getClickEventValue(activePopupClickEvent);
            Minecraft.getInstance().keyboardHandler.setClipboard(val);
            showNotification(HelpBoxLang.str("helpbox.ui.console_screen.notif.cmd_copied", val));
        }
        closePopup();
    }

    private void sendCommand(String text) {
        if (Minecraft.getInstance().player != null) {
            if (text.startsWith("/")) {
                Minecraft.getInstance().player.connection.sendCommand(text.substring(1));
            } else {
                Minecraft.getInstance().player.connection.sendChat(text);
            }
            showNotification(HelpBoxLang.str("helpbox.ui.console_screen.notif.sent", text));
        }
    }

    private void copySelectedText() {
        String selected = getSelectedTextString();
        if (!selected.isEmpty()) {
            Minecraft.getInstance().keyboardHandler.setClipboard(selected);
            showNotification(HelpBoxLang.str("helpbox.ui.console_screen.notif.text_copied", selected.length()));
        } else {
            showNotification(HelpBoxLang.str("helpbox.ui.console_screen.notif.no_selection"));
        }
    }

    private void copyAllText() {
        List<ConsoleHistory.ConsoleEntry> filtered = getFilteredEntries();
        StringBuilder sb = new StringBuilder();
        for (ConsoleHistory.ConsoleEntry entry : filtered) {
            sb.append("[").append(entry.timeStr()).append("] ").append(entry.plainText()).append("\n");
        }
        if (sb.length() > 0) {
            Minecraft.getInstance().keyboardHandler.setClipboard(sb.toString());
            showNotification(HelpBoxLang.str("helpbox.ui.console_screen.notif.all_copied", filtered.size()));
        }
    }

    private String getSelectedTextString() {
        if (selectionStartLine < 0 || selectionEndLine < 0) return "";
        List<ConsoleHistory.ConsoleEntry> filtered = getFilteredEntries();
        if (filtered.isEmpty()) return "";

        int startL = Math.min(selectionStartLine, selectionEndLine);
        int endL = Math.max(selectionStartLine, selectionEndLine);

        int startC = (selectionStartLine <= selectionEndLine) ? selectionStartChar : selectionEndChar;
        int endC = (selectionStartLine <= selectionEndLine) ? selectionEndChar : selectionStartChar;

        if (startL == endL && startC > endC) {
            int tmp = startC;
            startC = endC;
            endC = tmp;
        }

        startL = Math.max(0, Math.min(filtered.size() - 1, startL));
        endL = Math.max(0, Math.min(filtered.size() - 1, endL));

        StringBuilder sb = new StringBuilder();
        for (int l = startL; l <= endL; l++) {
            ConsoleHistory.ConsoleEntry entry = filtered.get(l);
            String line = "[" + entry.timeStr() + "] " + entry.plainText();

            int sIdx = (l == startL) ? Math.min(line.length(), Math.max(0, startC)) : 0;
            int eIdx = (l == endL) ? Math.min(line.length(), Math.max(0, endC)) : line.length();

            if (sIdx < eIdx) {
                sb.append(line, sIdx, eIdx);
            }
            if (l < endL) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private void showNotification(String text) {
        notificationText = text;
        notificationTimeMs = System.currentTimeMillis();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0xD010141D);
        super.extractBackground(ctx, mouseX, mouseY, delta);

        int contentTop = getContentTop();
        int contentBottom = getContentBottom();
        int contentLeft = getContentLeft();
        int contentRight = getContentRight();

        // Content area background
        ctx.fill(contentLeft, contentTop, contentRight, contentBottom, 0x90000000);
        ctx.fill(contentLeft, contentTop, contentRight, contentTop + 1, 0xFF334466);
        ctx.fill(contentLeft, contentBottom - 1, contentRight, contentBottom, 0xFF334466);

        List<ConsoleHistory.ConsoleEntry> filtered = getFilteredEntries();
        int lineHeight = this.font.lineHeight + 4;

        // Calculate selection bounds for highlight rendering
        int startL = Math.min(selectionStartLine, selectionEndLine);
        int endL = Math.max(selectionStartLine, selectionEndLine);
        int startC = (selectionStartLine <= selectionEndLine) ? selectionStartChar : selectionEndChar;
        int endC = (selectionStartLine <= selectionEndLine) ? selectionEndChar : selectionStartChar;

        if (startL == endL && startC > endC) {
            int tmp = startC;
            startC = endC;
            endC = tmp;
        }

        int sbX = getScrollbarX();
        ctx.enableScissor(contentLeft + 2, contentTop + 2, sbX - 3, contentBottom - 2);

        int currentY = contentTop + 4 - (int) scrollAmount;

        for (int i = 0; i < filtered.size(); i++) {
            if (currentY + lineHeight >= contentTop && currentY <= contentBottom) {
                ConsoleHistory.ConsoleEntry entry = filtered.get(i);
                String fullLine = "[" + entry.timeStr() + "] " + entry.plainText();
                int lineX = 15;

                // Render Underline / Cyan Highlight for Clickable Spans
                List<ClickableSpan> spans = getClickableSpans(entry);
                for (ClickableSpan span : spans) {
                    int cS = Math.min(fullLine.length(), Math.max(0, span.startChar()));
                    int cE = Math.min(fullLine.length(), Math.max(0, span.endChar()));
                    if (cS < cE) {
                        String prefix = fullLine.substring(0, cS);
                        String spanSub = fullLine.substring(cS, cE);
                        int spanX1 = lineX + this.font.width(prefix);
                        int spanW = this.font.width(spanSub);
                        // Underline indicator for clickable text
                        ctx.fill(spanX1, currentY + this.font.lineHeight, spanX1 + spanW, currentY + this.font.lineHeight + 1, 0xFF00E5FF);
                    }
                }

                // Draw selection highlight
                if (startL != -1 && i >= startL && i <= endL) {
                    int selS = (i == startL) ? Math.min(fullLine.length(), Math.max(0, startC)) : 0;
                    int selE = (i == endL) ? Math.min(fullLine.length(), Math.max(0, endC)) : fullLine.length();

                    if (selS < selE) {
                        String prefix = fullLine.substring(0, selS);
                        String selectedSub = fullLine.substring(selS, selE);
                        int highlightX1 = lineX + this.font.width(prefix);
                        int highlightW = this.font.width(selectedSub);
                        ctx.fill(highlightX1, currentY - 1, highlightX1 + highlightW, currentY + this.font.lineHeight + 1, 0x803366CC);
                    }
                }

                // Draw timestamp in gray, message with original formatting
                String timePrefix = "[" + entry.timeStr() + "] ";
                ctx.text(this.font, timePrefix, lineX, currentY, 0xFF888888, false);
                ctx.text(this.font, entry.component(), lineX + this.font.width(timePrefix), currentY, 0xFFFFFFFF, false);
            }
            currentY += lineHeight;
        }

        ctx.disableScissor();

        // --- Render Scrollbar ---
        int maxScroll = getMaxScroll();
        if (maxScroll > 0) {
            int sbY = getScrollbarY();
            int sbW = getScrollbarWidth();
            int sbH = getScrollbarHeight();

            // Track background
            ctx.fill(sbX, sbY, sbX + sbW, sbY + sbH, 0x40101824);

            int viewHeight = contentBottom - contentTop;
            int totalHeight = filtered.size() * lineHeight + 8;
            int thumbHeight = getThumbHeight(viewHeight, totalHeight, sbH);
            int thumbY = getThumbY(maxScroll, sbY, sbH, thumbHeight);

            boolean isHovered = mouseX >= sbX - 2 && mouseX <= sbX + sbW + 2 && mouseY >= sbY && mouseY <= sbY + sbH;
            int thumbColor = isDraggingScrollbar ? 0xFF5896FF : (isHovered ? 0xFF8AA8CE : 0xFF506886);

            // Thumb
            ctx.fill(sbX, thumbY, sbX + sbW, thumbY + thumbHeight, thumbColor);
            ctx.fill(sbX + 1, thumbY + 1, sbX + sbW - 1, thumbY + thumbHeight - 1, (thumbColor & 0x00FFFFFF) | 0x88000000);
        }

        // --- Render Floating Action Popup ("Git" Button Box) ---
        if (activePopupClickEvent != null) {
            int popupWidth = 320;
            int popupHeight = 65;
            int popupX = (this.width - popupWidth) / 2;
            int popupY = this.height - 115;

            // Panel Background & Borders (Modern Rounded Card)
            ModernUiRenderHelper.drawModernCard(ctx, popupX, popupY, popupWidth, popupHeight, 8, 0xF2101420, 0x8800E5FF);

            String actionTypeStr = HelpBoxLang.str("helpbox.ui.console_screen.action_type_command", "Komut:");
            if (activePopupClickEvent instanceof ClickEvent.OpenUrl) actionTypeStr = HelpBoxLang.str("helpbox.ui.console_screen.action_type_link", "Link:");
            else if (activePopupClickEvent instanceof ClickEvent.SuggestCommand) actionTypeStr = HelpBoxLang.str("helpbox.ui.console_screen.action_type_suggest", "\u00d6neri:");
            else if (activePopupClickEvent instanceof ClickEvent.CopyToClipboard) actionTypeStr = HelpBoxLang.str("helpbox.ui.console_screen.copy_cmd", "Kopyala:");

            String valStr = getClickEventValue(activePopupClickEvent);
            if (valStr.length() > 38) valStr = valStr.substring(0, 35) + "...";

            ctx.text(this.font, "\u00a7e\u00a7l" + HelpBoxLang.str("helpbox.ui.console_screen.clickable_action", "T\u0131klanabilir Eylem: %s", activePopupSpanText), popupX + 10, popupY + 6, 0xFFFFFFFF, false);
            ctx.text(this.font, "\u00a7b" + actionTypeStr + " \u00a77" + valStr, popupX + 10, popupY + 20, 0xFFAAAAAA, false);
        }

        // Toast / Notification Text
        if (!notificationText.isEmpty() && System.currentTimeMillis() - notificationTimeMs < 3000) {
            int notifW = this.font.width(notificationText) + 16;
            int notifX = (this.width - notifW) / 2;
            int notifY = (activePopupClickEvent != null) ? (this.height - 145) : (contentBottom - 30);
            ModernUiRenderHelper.fillRoundedRect(ctx, notifX, notifY, notifW, 20, 6, 0xE010B981);
            ctx.text(this.font, notificationText, notifX + 8, notifY + 6, 0xFFFFFFFF, false);
        }
    }
}
