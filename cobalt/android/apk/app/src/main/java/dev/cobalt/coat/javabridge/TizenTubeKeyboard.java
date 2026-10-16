// Copyright 2026 The TizenTube Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package dev.cobalt.coat.javabridge;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import dev.cobalt.coat.CobaltActivity;
import dev.cobalt.shell.Shell;
import org.chromium.ui.base.ViewAndroidDelegate;

/**
 * TizenTube: exposes a way for the userscript to summon the Android on-screen
 * keyboard (Gboard) for a focused text field. Cobalt's TV shell never shows the
 * IME for web inputs on its own, so a userscript dialog calls
 * {@code TizenTubeKeyboard.show()} when it opens an input.
 *
 * <p>The keyboard must target the ViewAndroidDelegate container (the render
 * view), which owns the ImeAdapter-backed InputConnection. Targeting any other
 * view makes Gboard fall back to raw key events, so capitals/Enter are lost.
 */
public class TizenTubeKeyboard implements CobaltJavaScriptAndroidObject {
  private final Context mContext;

  public TizenTubeKeyboard(Context context) {
    mContext = context;
  }

  @Override
  public String getJavaScriptInterfaceName() {
    return "TizenTubeKeyboard";
  }

  /** Returns the view that owns the web contents' IME input connection, if any. */
  private static View getImeTargetView(Activity activity) {
    if (activity instanceof CobaltActivity) {
      Shell shell = ((CobaltActivity) activity).getActiveShell();
      if (shell != null) {
        ViewAndroidDelegate delegate = shell.getViewAndroidDelegate();
        if (delegate != null && delegate.getContainerView() != null) {
          return delegate.getContainerView();
        }
      }
    }
    return activity.getCurrentFocus();
  }

  /** Shows the soft keyboard, targeting the render view that owns the IME connection. */
  @CobaltJavaScriptInterface
  public boolean show() {
    if (!(mContext instanceof Activity)) {
      return false;
    }
    final Activity activity = (Activity) mContext;
    activity.runOnUiThread(
        () -> {
          View view = getImeTargetView(activity);
          if (view == null) {
            view = activity.getWindow().getDecorView();
          }
          view.requestFocus();
          InputMethodManager imm =
              (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
          if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_FORCED);
          }
        });
    return true;
  }

  /** Hides the soft keyboard. */
  @CobaltJavaScriptInterface
  public boolean hide() {
    if (!(mContext instanceof Activity)) {
      return false;
    }
    final Activity activity = (Activity) mContext;
    activity.runOnUiThread(
        () -> {
          InputMethodManager imm =
              (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
          if (imm != null) {
            View view = getImeTargetView(activity);
            if (view == null) {
              view = activity.getWindow().getDecorView();
            }
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
          }
        });
    return true;
  }
}
