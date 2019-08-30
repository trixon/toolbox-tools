/*
 * Copyright 2019 Patrik Karlström.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.trixon.tools.mapollage.ui.config;

import java.util.Locale;
import java.util.ResourceBundle;
import javafx.geometry.Insets;
import javafx.scene.control.Tab;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import org.controlsfx.validation.ValidationSupport;
import se.trixon.almond.util.SystemHelper;
import se.trixon.toolbox.api.TbPreferences;
import se.trixon.tools.mapollage.ProfileManager;
import se.trixon.tools.mapollage.profile.Profile;

/**
 *
 * @author Patrik Karlström
 */
public abstract class BaseTab extends Tab {

    public static final int ICON_SIZE = 32;
    public static final String MULTILINE_DIVIDER = "* * * * *";
    protected static ValidationSupport sValidationSupport;
    protected final ResourceBundle mBundle = SystemHelper.getBundle(BaseTab.class, "Bundle");
    protected final String mHeaderPrefix = " + ";
    protected final TbPreferences mTbPreferences = TbPreferences.getInstance();
    protected Profile mProfile;
    protected final ProfileManager mProfileManager = ProfileManager.getInstance();
    protected String mTitle;
    private final Insets mTopInsets = new Insets(8, 0, 0, 0);

    public static void setValidationSupport(ValidationSupport validationSupport) {
        BaseTab.sValidationSupport = validationSupport;
    }

    public BaseTab() {
    }

    public Locale getDateFormatLocale() {
        return mTbPreferences.general().getLocale();
    }

    public String getTitle() {
        return mTitle;
    }

    public abstract void load();

    public abstract void save();

    public void setTitle(String title) {
        mTitle = title;
    }

    protected void addTopMargin(Region... regions) {
        for (Region region : regions) {
            GridPane.setMargin(region, mTopInsets);
        }
    }

    protected void addTopPadding(Region... regions) {
        for (Region region : regions) {
            region.setPadding(mTopInsets);
        }
    }

    protected void append(StringBuilder sb, String key, String value) {
        sb.append(mHeaderPrefix).append(String.format("%s: %s\n", key, value));
    }

    protected void invalidSettings(String message) {
        //Message.error(this, Dict.INVALID_SETTING.toString(), String.format("<html><h3>%s</h3>%s", mTitle, message));
    }

    protected void optAppend(StringBuilder sb, boolean state, String string) {
        if (state) {
            sb.append(mHeaderPrefix).append(string).append("\n");
        }
    }
}
