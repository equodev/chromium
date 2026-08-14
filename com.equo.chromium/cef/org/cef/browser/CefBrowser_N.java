// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.OS;
import org.cef.browser.CefDevToolsClient.DevToolsException;
import org.cef.callback.CefDragData;
import org.cef.callback.CefNativeAdapter;
import org.cef.callback.CefPdfPrintCallback;
import org.cef.callback.CefRunFileDialogCallback;
import org.cef.callback.CefStringVisitor;
import org.cef.handler.CefClientHandler;
import org.cef.handler.CefDialogHandler.FileDialogMode;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefWindowHandler;
import org.cef.misc.CefPdfPrintSettings;
import org.cef.misc.Point;
import org.cef.misc.Rectangle;
import org.cef.network.CefRequest;

import java.util.Vector;
import java.util.concurrent.CompletableFuture;

/**
 * This class represents all methods which are connected to the
 * native counterpart CEF.
 * The visibility of this class is "package". To create a new
 * CefBrowser instance, please use CefBrowserFactory.
 */
abstract class CefBrowser_N extends CefNativeAdapter implements CefBrowser {
    protected volatile boolean isPending_ = false;
    private final CefClient client_;
    private final String url_;
    private final CefRequestContext request_context_;
    protected volatile CefBrowser_N parent_ = null;
    private volatile Point inspectAt_ = null;
    private volatile CefDevToolsClient devToolsClient_ = null;
    private boolean closeAllowed_ = false;
    private volatile boolean isClosed_ = false;
    private volatile boolean isClosing_ = false;
    private Object reference;
    private final CefBrowserSettings settings_;
    protected long jGlobalRef_ = 0;

    protected CefBrowser_N(CefClient client, String url, CefRequestContext context,
            CefBrowser_N parent, Point inspectAt, CefBrowserSettings settings) {
        client_ = client;
        url_ = url;
        request_context_ = context;
        parent_ = parent;
        inspectAt_ = inspectAt;
        if (settings != null)
            settings_ = settings.clone();
        else
            settings_ = new CefBrowserSettings();
    }

    protected String getUrl() {
        return url_;
    }

    @Override
    public CefRequestContext getRequestContext() {
        return request_context_ != null ? request_context_ : CefRequestContext.getGlobalContext();
    }

    protected CefBrowser_N getParentBrowser() {
        return parent_;
    }

    protected Point getInspectAt() {
        return inspectAt_;
    }

    protected boolean isClosed() {
        return isClosed_;
    }

    @Override
    public CefClient getClient() {
        return client_;
    }

    @Override
    public CefRenderHandler getRenderHandler() {
        return null;
    }

    @Override
    public CefWindowHandler getWindowHandler() {
        return null;
    }

    @Override
    public synchronized void setCloseAllowed() {
        closeAllowed_ = true;
    }

    @Override
    public synchronized boolean doClose() {
        if (closeAllowed_) {
            // Allow the close to proceed.
            return false;
        }

        // Cancel the close.
        return true;
    }

    @Override
    public synchronized void onBeforeClose() {
        isClosed_ = true;
        if (request_context_ != null) request_context_.dispose();
        if (parent_ != null) {
            parent_.closeDevTools();
            parent_ = null;
        }
        if (devToolsClient_ != null) {
            devToolsClient_.close();
        }
    }

    public boolean isClosedOrClosing() {
        return (isClosed_ || isClosing_);
    }

    @Override
    public void openDevTools() {
        openDevTools(null);
    }

    @Override
    public synchronized void openDevTools(Point inspectAt) {
        createDevToolsBrowser(client_, url_, request_context_, this, inspectAt).createImmediately();
    }

    @Override
    public synchronized CefDevToolsClient getDevToolsClient() {
        if (!isPending_ || isClosing_ || isClosed_) {
            return null;
        }
        if (devToolsClient_ == null || devToolsClient_.isClosed()) {
            devToolsClient_ = new CefDevToolsClient(this);
        }
        return devToolsClient_;
    }

    CompletableFuture<Integer> executeDevToolsMethod(String method, String parametersAsJson) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        N_ExecuteDevToolsMethod(method, parametersAsJson, new IntCallback() {
            @Override
            public void onComplete(int generatedMessageId) {
                if (generatedMessageId <= 0) {
                    future.completeExceptionally(new DevToolsException(
                            String.format("Failed to execute DevTools method %s", method)));
                } else {
                    future.complete(generatedMessageId);
                }
            }
        });
        return future;
    }

    protected abstract CefBrowser_N createDevToolsBrowser(CefClient client, String url,
            CefRequestContext context, CefBrowser_N parent, Point inspectAt);

    protected boolean isFramelessWindow() {
        return false;
    }

    /**
     * Create a new browser.
     */
    protected void createBrowser(CefClientHandler clientHandler, long windowHandle, String url,
            boolean osr, boolean transparent, Object canvas, CefRequestContext context) {
            createBrowser(clientHandler, windowHandle, url, osr, transparent, canvas, context, "");
    }

    /**
     * Create a new browser with title.
     */
    protected void createBrowser(CefClientHandler clientHandler, long windowHandle, String url,
            boolean osr, boolean transparent, Object canvas, CefRequestContext context, String title) {
        if (getNativeRef("CefBrowser") == 0 && !isPending_) {
            try {
                N_CreateBrowser(clientHandler, windowHandle, url, osr, transparent, canvas, context,
                        settings_, title, isFramelessWindow());
            } catch (UnsatisfiedLinkError err) {
                err.printStackTrace();
            }
        }
    }

    /**
     * Called async from the (native) main UI thread.
     */
    private void notifyBrowserCreated(long jGlobalRef) {
        isPending_ = true;
        if (OS.isWindows()) {
            jGlobalRef_ = jGlobalRef;
        }
    }

    /**
     * Create a new browser as dev tools
     */
    protected final void createDevTools(CefBrowser_N parent, CefClientHandler clientHandler,
            long windowHandle, boolean osr, boolean transparent, Object canvas, Point inspectAt) {
        if (getNativeRef("CefBrowser") == 0 && !isPending_) {
            try {
                isPending_ = N_CreateDevTools(
                        parent, clientHandler, windowHandle, osr, transparent, canvas, inspectAt);
            } catch (UnsatisfiedLinkError err) {
                err.printStackTrace();
            }
        }
    }

    public void setFullscreen(boolean fullscreen) {
        N_SetFullscreen(fullscreen);
    }

    public void setWindowIcon(String iconPath) {
        N_SetWindowIcon(iconPath);
    }

    public void setWindowTitle(String title) {
        N_SetWindowTitle(title);
    }

    public void maximize(){
        N_Maximize();
    }

    public void minimize(){
        N_Minimize();
    }

    public void restore() {
        N_Restore();
    }
    
    @Override
    public void sendDevToolsMessage(String message, long messageSize) {
        try {
            N_SendDevToolsMessage(message, messageSize);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public CefRegistration addDevToolsMessageObserver(CefDevToolsMessageObserver observer) {
        try {
            return N_AddDevToolsMessageObserver(observer);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return null;
    }

    @Override
    public void removeDevToolsMessageObservers(CefRegistration cefRegistration) {
        N_RemoveDevToolsMessageObserver(cefRegistration);
    }

    /**
     * Returns the native window handle for the specified native surface handle.
     */
    protected final long getWindowHandle(long surfaceHandle) {
        try {
            return N_GetWindowHandle(surfaceHandle);
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
        return 0;
    }

    public final long getNativeWindowHandle() {
        try {
            return N_GetNativeWindowHandle();
        } catch (UnsatisfiedLinkError err) {
            err.printStackTrace();
        }
        return 0;
    }

    @Override
    protected void finalize() throws Throwable {
        close(true);
        super.finalize();
    }

    @Override
    public boolean canGoBack() {
        try {
            return N_CanGoBack();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public void goBack() {
        try {
            N_GoBack();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public boolean canGoForward() {
        try {
            return N_CanGoForward();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public void goForward() {
        try {
            N_GoForward();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public boolean isLoading() {
        try {
            return N_IsLoading();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public void reload() {
        try {
            N_Reload();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void reloadIgnoreCache() {
        try {
            N_ReloadIgnoreCache();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void stopLoad() {
        try {
            N_StopLoad();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public int getIdentifier() {
        try {
            return N_GetIdentifier();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return -1;
        }
    }

    @Override
    public CefFrame getMainFrame() {
        try {
            return N_GetMainFrame();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return null;
        }
    }

    @Override
    public CefFrame getFocusedFrame() {
        try {
            return N_GetFocusedFrame();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return null;
        }
    }

    @Override
    public CefFrame getFrameByIdentifier(String identifier) {
        try {
            return N_GetFrameByIdentifier(identifier);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return null;
        }
    }

    @Override
    public CefFrame getFrameByName(String name) {
        try {
            return N_GetFrameByName(name);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return null;
        }
    }

    @Override
    public Vector<String> getFrameIdentifiers() {
        try {
            return N_GetFrameIdentifiers();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return null;
        }
    }

    @Override
    public Vector<String> getFrameNames() {
        try {
            return N_GetFrameNames();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return null;
        }
    }

    @Override
    public int getFrameCount() {
        try {
            return N_GetFrameCount();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            return -1;
        }
    }

    @Override
    public boolean isPopup() {
        try {
            return N_IsPopup();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean hasDocument() {
        try {
            return N_HasDocument();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public void viewSource() {
        try {
            N_ViewSource();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void getSource(CefStringVisitor visitor) {
        try {
            N_GetSource(visitor);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void getText(CefStringVisitor visitor) {
        try {
            N_GetText(visitor);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void loadRequest(CefRequest request) {
        try {
            N_LoadRequest(request);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void loadURL(String url) {
        try {
            N_LoadURL(url);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void executeJavaScript(String code, String url, int line) {
        try {
            N_ExecuteJavaScript(code, url, line);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void sendEvalMessage(String file, String eval) {
        try {
            N_SendEvalMessage(file, eval);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public String getURL() {
        try {
            return N_GetURL();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return "";
    }

    @Override
    public void close(boolean force) {
        if (isClosing_ || isClosed_) return;
        if (force) isClosing_ = true;

        try {
            N_Close(force, isFramelessWindow());
            if (jGlobalRef_ > 0) {
                N_RemoveJGlobalRef(client_, jGlobalRef_);
            }
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    private boolean isHandlingFocus = false;

    @Override
    public void setFocus(boolean enable) {
        if (isHandlingFocus) {
            return;
        }
        isHandlingFocus = true;
        try {
            N_SetFocus(enable);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        } catch (StackOverflowError e) {
        } finally {
            isHandlingFocus = false;
        }
    }

    @Override
    public void setWindowVisibility(boolean visible) {
        try {
            N_SetWindowVisibility(visible);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public double getZoomLevel() {
        try {
            return N_GetZoomLevel();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return 0.0;
    }

    @Override
    public void setZoomLevel(double zoomLevel) {
        try {
            N_SetZoomLevel(zoomLevel);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void runFileDialog(FileDialogMode mode, String title, String defaultFilePath,
            Vector<String> acceptFilters, int selectedAcceptFilter,
            CefRunFileDialogCallback callback) {
        try {
            N_RunFileDialog(
                    mode, title, defaultFilePath, acceptFilters, selectedAcceptFilter, callback);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void startDownload(String url) {
        try {
            N_StartDownload(url);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void print() {
        try {
            N_Print();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void printToPDF(
            String path, CefPdfPrintSettings settings, CefPdfPrintCallback callback) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path was null or empty");
        }
        try {
            N_PrintToPDF(path, settings, callback);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void find(String searchText, boolean forward, boolean matchCase, boolean findNext) {
        try {
            N_Find(searchText, forward, matchCase, findNext);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void stopFinding(boolean clearSelection) {
        try {
            N_StopFinding(clearSelection);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public void closeDevTools() {
        try {
            N_CloseDevTools();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    @Override
    public boolean hasDevTools() {
        try {
            return N_HasDevTools();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
        return false;
    }

    @Override
    public void replaceMisspelling(String word) {
        try {
            N_ReplaceMisspelling(word);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Notify that the browser was resized.
     * @param width The new width of the browser
     * @param height The new height of the browser
     */
    protected final void wasResized(int width, int height, boolean forceRepaint) {
        try {
            N_WasResized(width, height, forceRepaint);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    public final void setWindowSize(int width, int height, boolean forceRepaint) {
        try {
            N_SetWindowSize(width, height, forceRepaint);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    public final void setWindowBounds(int x, int y, int width, int height) {
        try {
            N_SetWindowBounds(x, y, width, height);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    public final void setWindowPosition(int x, int y) {
        try {
            N_SetWindowPosition(x, y);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    protected final void initialSize(int width, int height, boolean forceRepaint) {
        try {
            N_InitialSize(width, height, forceRepaint);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Invalidate the UI.
     */
    protected final void invalidate() {
        try {
            N_Invalidate();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Send a key event.
     * @param e The event to send.
     */
    protected final void sendKeyEvent(Object e) {
        try {
            N_SendKeyEvent(e);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Send a mouse event.
     * @param e The event to send.
     */
    protected final void sendMouseEvent(Object e) {
        try {
            N_SendMouseEvent(e);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Send a mouse wheel event.
     * @param e The event to send.
     */
    protected final void sendMouseWheelEvent(Object e) {
        try {
            N_SendMouseWheelEvent(e);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Call this method when the user drags the mouse into the web view (before
     * calling DragTargetDragOver/DragTargetLeave/DragTargetDrop).
     * |drag_data| should not contain file contents as this type of data is not
     * allowed to be dragged into the web view. File contents can be removed using
     * CefDragData::ResetFileContents (for example, if |drag_data| comes from
     * CefRenderHandler::StartDragging).
     * This method is only used when window rendering is disabled.
     */
    protected final void dragTargetDragEnter(
            CefDragData dragData, Point pos, int modifiers, int allowedOps) {
        try {
            N_DragTargetDragEnter(dragData, pos, modifiers, allowedOps);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Call this method each time the mouse is moved across the web view during
     * a drag operation (after calling DragTargetDragEnter and before calling
     * DragTargetDragLeave/DragTargetDrop).
     * This method is only used when window rendering is disabled.
     */
    protected final void dragTargetDragOver(Point pos, int modifiers, int allowedOps) {
        try {
            N_DragTargetDragOver(pos, modifiers, allowedOps);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Call this method when the user drags the mouse out of the web view (after
     * calling DragTargetDragEnter).
     * This method is only used when window rendering is disabled.
     */
    protected final void dragTargetDragLeave() {
        try {
            N_DragTargetDragLeave();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Call this method when the user completes the drag operation by dropping
     * the object onto the web view (after calling DragTargetDragEnter).
     * The object being dropped is |drag_data|, given as an argument to
     * the previous DragTargetDragEnter call.
     * This method is only used when window rendering is disabled.
     */
    protected final void dragTargetDrop(Point pos, int modifiers) {
        try {
            N_DragTargetDrop(pos, modifiers);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Call this method when the drag operation started by a
     * CefRenderHandler.startDragging call has ended either in a drop or
     * by being cancelled. |x| and |y| are mouse coordinates relative to the
     * upper-left corner of the view. If the web view is both the drag source
     * and the drag target then all DragTarget* methods should be called before
     * DragSource* methods.
     * This method is only used when window rendering is disabled.
     */
    protected final void dragSourceEndedAt(Point pos, int operation) {
        try {
            N_DragSourceEndedAt(pos, operation);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Call this method when the drag operation started by a
     * CefRenderHandler.startDragging call has completed. This method may be
     * called immediately without first calling DragSourceEndedAt to cancel a
     * drag operation. If the web view is both the drag source and the drag
     * target then all DragTarget* methods should be called before DragSource*
     * methods.
     * This method is only used when window rendering is disabled.
     */
    protected final void dragSourceSystemDragEnded() {
        try {
            N_DragSourceSystemDragEnded();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    protected final void updateUI(Rectangle contentRect, Rectangle browserRect, long handle) {
        try {
            N_UpdateUI(contentRect, browserRect, handle);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    protected final void setParent(long windowHandle, Object canvas) {
        if (isClosing_ || isClosed_) return;

        try {
            N_SetParent(windowHandle, canvas);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    /**
     * Call this method if the browser frame was moved.
     * This fixes positioning of select popups and dismissal on window move/resize.
     */
    protected final void notifyMoveOrResizeStarted() {
        try {
            N_NotifyMoveOrResizeStarted();
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    public void setWindowlessFrameRate(int frameRate) {
        try {
            N_SetWindowlessFrameRate(frameRate);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
        }
    }

    public void setReference(Object reference) {
        this.reference = reference;
    }

    public Object getReference() {
        return this.reference;
    }

    public CompletableFuture<Integer> getWindowlessFrameRate() {
        final CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            N_GetWindowlessFrameRate(future::complete);
        } catch (UnsatisfiedLinkError ule) {
            ule.printStackTrace();
            future.complete(0);
        }
        return future;
    }

    private interface IntCallback {
        void onComplete(int value);
    }

    private final native boolean N_CreateBrowser(CefClientHandler clientHandler, long windowHandle,
            String url, boolean osr, boolean transparent, Object canvas, CefRequestContext context,
            CefBrowserSettings settings);
    private final native boolean N_CreateBrowser(CefClientHandler clientHandler, long windowHandle,
            String url, boolean osr, boolean transparent, Object canvas,
            CefRequestContext context, CefBrowserSettings settings, String title,
            boolean frameless);
    private final native boolean N_CreateDevTools(CefBrowser parent, CefClientHandler clientHandler,
            long windowHandle, boolean osr, boolean transparent, Object canvas, Point inspectAt);
    private final native void N_ExecuteDevToolsMethod(
            String method, String parametersAsJson, IntCallback callback);
    private final native CefRegistration N_AddDevToolsMessageObserver(
            CefDevToolsMessageObserver observer);
    private final native void N_SendDevToolsMessage(String message, long messageSize);
    private final native void N_RemoveDevToolsMessageObserver(CefRegistration cefRegistration);
    private final native long N_GetWindowHandle(long surfaceHandle);
    private final native void N_SetFullscreen(boolean fullscreen);
    private final native void N_SetWindowIcon(String iconPath);
    private final native void N_SetWindowTitle(String title);
    private final native void N_Maximize();
    private final native void N_Minimize();
    private final native void N_Restore();
    private final native long N_GetNativeWindowHandle();
    private final native boolean N_CanGoBack();
    private final native void N_GoBack();
    private final native boolean N_CanGoForward();
    private final native void N_GoForward();
    private final native boolean N_IsLoading();
    private final native void N_Reload();
    private final native void N_ReloadIgnoreCache();
    private final native void N_StopLoad();
    private final native int N_GetIdentifier();
    private final native CefFrame N_GetMainFrame();
    private final native CefFrame N_GetFocusedFrame();
    private final native CefFrame N_GetFrameByIdentifier(String identifier);
    private final native CefFrame N_GetFrameByName(String name);
    private final native Vector<String> N_GetFrameIdentifiers();
    private final native Vector<String> N_GetFrameNames();
    private final native int N_GetFrameCount();
    private final native boolean N_IsPopup();
    private final native boolean N_HasDocument();
    private final native void N_ViewSource();
    private final native void N_GetSource(CefStringVisitor visitor);
    private final native void N_GetText(CefStringVisitor visitor);
    private final native void N_LoadRequest(CefRequest request);
    private final native void N_LoadURL(String url);
    private final native void N_ExecuteJavaScript(String code, String url, int line);
    private final native void N_SendEvalMessage(String file, String eval);
    private final native String N_GetURL();
    private final native void N_Close(boolean force, boolean frameless);
    private final native void N_RemoveJGlobalRef(CefClientHandler clientHandler, long jGlobalRef_);
    private final native void N_SetFocus(boolean enable);
    private final native void N_SetWindowVisibility(boolean visible);
    private final native double N_GetZoomLevel();
    private final native void N_SetZoomLevel(double zoomLevel);
    private final native void N_RunFileDialog(FileDialogMode mode, String title,
            String defaultFilePath, Vector<String> acceptFilters, int selectedAcceptFilter,
            CefRunFileDialogCallback callback);
    private final native void N_StartDownload(String url);
    private final native void N_Print();
    private final native void N_PrintToPDF(
            String path, CefPdfPrintSettings settings, CefPdfPrintCallback callback);
    private final native void N_Find(
            String searchText, boolean forward, boolean matchCase, boolean findNext);
    private final native void N_StopFinding(boolean clearSelection);
    private final native void N_CloseDevTools();
    private final native boolean N_HasDevTools();
    private final native void N_ReplaceMisspelling(String word);
    private final native void N_SetWindowSize(int width, int height, boolean forceRepaint);
    private final native void N_SetWindowBounds(int x, int y, int width, int height);
    private final native void N_SetWindowPosition(int x, int y);
    private final native void N_WasResized(int width, int height, boolean forceRepaint);
    private final native void N_InitialSize(int width, int height, boolean forceRepaint);
    private final native void N_Invalidate();
    private final native void N_SendKeyEvent(Object e);
    private final native void N_SendMouseEvent(Object e);
    private final native void N_SendMouseWheelEvent(Object e);
    private final native void N_DragTargetDragEnter(
            CefDragData dragData, Point pos, int modifiers, int allowed_ops);
    private final native void N_DragTargetDragOver(Point pos, int modifiers, int allowed_ops);
    private final native void N_DragTargetDragLeave();
    private final native void N_DragTargetDrop(Point pos, int modifiers);
    private final native void N_DragSourceEndedAt(Point pos, int operation);
    private final native void N_DragSourceSystemDragEnded();
    private final native void N_UpdateUI(Rectangle contentRect, Rectangle browserRect, long handle);
    private final native void N_SetParent(long windowHandle, Object canvas);
    private final native void N_NotifyMoveOrResizeStarted();
    private final native void N_SetWindowlessFrameRate(int frameRate);
    private final native void N_GetWindowlessFrameRate(IntCallback frameRateCallback);
}
