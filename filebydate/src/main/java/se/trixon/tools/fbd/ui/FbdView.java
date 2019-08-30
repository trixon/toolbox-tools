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
package se.trixon.tools.fbd.ui;

import com.dlsc.workbenchfx.Workbench;
import com.dlsc.workbenchfx.model.WorkbenchDialog;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.FadeTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToolBar;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;
import org.apache.commons.io.FileUtils;
import org.controlsfx.control.action.Action;
import org.controlsfx.control.action.ActionUtils;
import se.trixon.almond.util.Dict;
import se.trixon.almond.util.SystemHelper;
import se.trixon.almond.util.fx.FxHelper;
import se.trixon.almond.util.icons.material.MaterialIcon;
import se.trixon.toolbox.api.TbNameCase;
import se.trixon.toolbox.api.TbRunState;
import static se.trixon.toolbox.api.TbToolbox.*;
import se.trixon.tools.fbd.Operation;
import se.trixon.tools.fbd.OperationListener;
import se.trixon.tools.fbd.Profile;
import se.trixon.tools.fbd.ProfileManager;

/**
 *
 * @author Patrik Karlström
 */
public class FbdView extends BorderPane {

    private static final Logger LOGGER = Logger.getLogger(FbdView.class.getName());

    private final ResourceBundle mBundle = SystemHelper.getBundle(FbdView.class, "Bundle");
    private Font mDefaultFont;
    private final ProfileIndicator mIndicator = new ProfileIndicator();
    private final ObservableList<Profile> mItems = FXCollections.observableArrayList();
    private Profile mLastRunProfile;
    private ListView<Profile> mListView;
    private final FbdModule mModule;
    private OperationListener mOperationListener;
    private Thread mOperationThread;
    private PreviewPanel mPreviewPanel;
    private final ProfileManager mProfileManager = ProfileManager.getInstance();
    private LinkedList<Profile> mProfiles;
    private final ProgressPanel mProgressPanel = new ProgressPanel();
    private final Workbench mWorkbench;

    public FbdView(Workbench workbench, FbdModule module) {
        mWorkbench = workbench;
        mModule = module;
        createUI();
        postInit();
        initListeners();
        mListView.requestFocus();
    }

    void doCancel() {
        mOperationThread.interrupt();
    }

    void doNavHome() {
        setCenter(mListView);
    }

    void doNavLog() {
        setCenter(mProgressPanel);
    }

    void doRun() {
        profileRun(mLastRunProfile);
    }

    void profileEdit(Profile profile) {
        String title = Dict.EDIT.toString();
        boolean addNew = false;
        boolean clone = profile != null && profile.getName() == null;

        if (profile == null) {
            title = Dict.ADD.toString();
            addNew = true;
            profile = new Profile();
            profile.setSourceDir(FileUtils.getUserDirectory());
            profile.setDestDir(FileUtils.getUserDirectory());
            profile.setFilePattern("{*.jpg,*.JPG}");
            profile.setDatePattern("yyyy/MM/yyyy-MM-dd");
            profile.setOperation(0);
            profile.setFollowLinks(true);
            profile.setRecursive(true);
            profile.setReplaceExisting(false);
            profile.setCaseBase(TbNameCase.UNCHANGED);
            profile.setCaseExt(TbNameCase.UNCHANGED);
        } else if (clone) {
            title = Dict.CLONE.toString();
            profile.setLastRun(0);
        }

        ProfilePanel profilePanel = new ProfilePanel(profile);

        final boolean add = addNew;
        final Profile p = profile;
        WorkbenchDialog dialog = WorkbenchDialog.builder(title, profilePanel, ButtonType.CANCEL, ButtonType.OK).onResult(buttonType -> {
            if (buttonType == ButtonType.OK) {
                profilePanel.save();
                if (add || clone) {
                    mProfiles.add(p);
                }

                profilesSave();
                populateProfiles(p);
            }
        }).build();

        dialog.setOnShown(event -> {
            profilePanel.setOkButton(dialog.getButton(ButtonType.OK).get());
        });

        mWorkbench.showDialog(dialog);
    }

    private void createUI() {
        mDefaultFont = Font.getDefault();

        mListView = new ListView<>();
        mListView.setItems(mItems);
        mListView.setCellFactory((ListView<Profile> param) -> new ProfileListCell());
        Label welcomeLabel = new Label(mBundle.getString("welcome"));
        welcomeLabel.setFont(Font.font(mDefaultFont.getName(), FontPosture.ITALIC, 18));

        mListView.setPlaceholder(welcomeLabel);

        mPreviewPanel = new PreviewPanel();

        setCenter(mListView);
        setBottom(mPreviewPanel);
    }

    private void initListeners() {
        mOperationListener = new OperationListener() {
            private boolean mSuccess;

            @Override
            public void onOperationError(String message) {
                mProgressPanel.err(message);
            }

            @Override
            public void onOperationFailed(String message) {
                onOperationFinished(message, 0);
                mSuccess = false;
            }

            @Override
            public void onOperationFinished(String message, int fileCount) {
                mModule.setRunningState(TbRunState.CLOSEABLE);
                mProgressPanel.out(Dict.DONE.toString());
                populateProfiles(mLastRunProfile);

                if (0 == fileCount) {
                    mProgressPanel.setProgress(1);
                }
            }

            @Override
            public void onOperationInterrupted() {
                mModule.setRunningState(TbRunState.CLOSEABLE);
                mProgressPanel.setProgress(0);
                mSuccess = false;
            }

            @Override
            public void onOperationLog(String message) {
                mProgressPanel.out(message);
            }

            @Override
            public void onOperationProcessingStarted() {
                mProgressPanel.setProgress(-1);
            }

            @Override
            public void onOperationProgress(int value, int max) {
                mProgressPanel.setProgress(value / (double) max);
            }

            @Override
            public void onOperationStarted() {
                mModule.setRunningState(TbRunState.CANCELABLE);
                mProgressPanel.setProgress(0);
                mSuccess = true;
            }
        };

    }

    private void populateProfiles(Profile profile) {
        mItems.clear();
        Collections.sort(mProfiles);

        mProfiles.stream().forEach((item) -> {
            mItems.add(item);
        });

        if (profile != null) {
            mListView.getSelectionModel().select(profile);
            mListView.scrollTo(profile);
        }

        mPreviewPanel.setVisible(!mListView.getSelectionModel().isEmpty());
    }

    private void postInit() {
        profilesLoad();
        populateProfiles(null);
    }

    private void profileRemove(Profile profile) {
        String title = Dict.Dialog.TITLE_PROFILE_REMOVE.toString() + "?";
        String message = String.format(Dict.Dialog.MESSAGE_PROFILE_REMOVE.toString(), profile.getName());

        ButtonType removeButtonType = new ButtonType(Dict.REMOVE.toString(), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(Dict.CANCEL.toString(), ButtonBar.ButtonData.CANCEL_CLOSE);

        WorkbenchDialog dialog = WorkbenchDialog.builder(title, message, removeButtonType, cancelButtonType).onResult(buttonType -> {
            if (buttonType == removeButtonType) {
                mProfiles.remove(profile);
                profilesSave();
                populateProfiles(null);
//            mLogAction.setDisabled(mItems.isEmpty() || mLastRunProfile == null);//TODO Restore 2019-08-10
            }
        }).build();
        mWorkbench.showDialog(dialog);
    }

    private void profileRun(Profile profile) {
        PreviewPanel previewPanel = new PreviewPanel();
        previewPanel.load(profile);

        ButtonType runButtonType = new ButtonType(Dict.RUN.toString());
        ButtonType dryRunButtonType = new ButtonType(Dict.DRY_RUN.toString(), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType(Dict.CANCEL.toString(), ButtonBar.ButtonData.CANCEL_CLOSE);

        String title = String.format(Dict.Dialog.TITLE_PROFILE_RUN.toString(), profile.getName());

        WorkbenchDialog dialog = WorkbenchDialog.builder(title, previewPanel, runButtonType, dryRunButtonType, cancelButtonType).onResult(buttonType -> {
            if (buttonType != cancelButtonType) {
                boolean dryRun = buttonType == dryRunButtonType;
                profile.setDryRun(dryRun);
                mProgressPanel.clear();
                setCenter(mProgressPanel);
                mIndicator.setProfile(profile);

                if (profile.isValid()) {
                    mLastRunProfile = profile;
                    mOperationThread = new Thread(() -> {
                        Operation operation = new Operation(mOperationListener, profile);
                        operation.start();
                    });
                    mOperationThread.setName("Operation");
                    mOperationThread.start();
                } else {
                    mProgressPanel.out(profile.toDebugString());
                    mProgressPanel.out(profile.getValidationError());
                    mProgressPanel.out(Dict.ABORTING.toString());
                }
            }
        }).build();
        mWorkbench.showDialog(dialog);
    }

    private void profilesLoad() {
        try {
            mProfileManager.load();
            mProfiles = mProfileManager.getProfiles();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private void profilesSave() {
        try {
            mProfileManager.save();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    class ProfileListCell extends ListCell<Profile> {

        private final BorderPane mBorderPane = new BorderPane();
        private final Label mDescLabel = new Label();
        private final Duration mDuration = Duration.millis(200);
        private final FadeTransition mFadeInTransition = new FadeTransition();
        private final FadeTransition mFadeOutTransition = new FadeTransition();
        private final Label mLastLabel = new Label();
        private final Label mNameLabel = new Label();
        private final SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat();

        public ProfileListCell() {
            mFadeInTransition.setDuration(mDuration);
            mFadeInTransition.setFromValue(0);
            mFadeInTransition.setToValue(1);

            mFadeOutTransition.setDuration(mDuration);
            mFadeOutTransition.setFromValue(1);
            mFadeOutTransition.setToValue(0);

            createUI();
        }

        private void addContent(Profile profile) {
            setText(null);

            mNameLabel.setText(profile.getName());
            mDescLabel.setText(profile.getDescription());
            String lastRun = "-";
            if (profile.getLastRun() != 0) {
                lastRun = mSimpleDateFormat.format(new Date(profile.getLastRun()));
            }
            mLastLabel.setText(lastRun);

            setGraphic(mBorderPane);
        }

        private void clearContent() {
            setText(null);
            setGraphic(null);
        }

        private void createUI() {
            String fontFamily = mDefaultFont.getFamily();
            double fontSize = mDefaultFont.getSize();

            mNameLabel.setFont(Font.font(fontFamily, FontWeight.BOLD, fontSize * 2));
            mDescLabel.setFont(Font.font(fontFamily, FontWeight.NORMAL, fontSize * 1.3));
            mLastLabel.setFont(Font.font(fontFamily, FontWeight.NORMAL, fontSize * 1.3));

            Action runAction = new Action(Dict.RUN.toString(), (ActionEvent event) -> {
                profileRun(getSelectedProfile());
                mListView.requestFocus();
            });
            runAction.setGraphic(MaterialIcon._Av.PLAY_ARROW.getImageView(ICON_SIZE_PROFILE));

            Action editAction = new Action(Dict.EDIT.toString(), (ActionEvent event) -> {
                profileEdit(getSelectedProfile());
                mListView.requestFocus();
            });
            editAction.setGraphic(MaterialIcon._Image.EDIT.getImageView(ICON_SIZE_PROFILE));

            Action cloneAction = new Action(Dict.CLONE.toString(), (ActionEvent event) -> {
                profileClone();
                mListView.requestFocus();
            });
            cloneAction.setGraphic(MaterialIcon._Content.CONTENT_COPY.getImageView(ICON_SIZE_PROFILE));

            Action removeAction = new Action(Dict.REMOVE.toString(), (ActionEvent event) -> {
                profileRemove(getSelectedProfile());
                mListView.requestFocus();
            });
            removeAction.setGraphic(MaterialIcon._Content.REMOVE.getImageView(ICON_SIZE_PROFILE));

            VBox mainBox = new VBox(mNameLabel, mDescLabel, mLastLabel);
            mainBox.setAlignment(Pos.CENTER_LEFT);

            Collection<? extends Action> actions = Arrays.asList(
                    runAction,
                    editAction,
                    cloneAction,
                    removeAction
            );

            ToolBar toolBar = ActionUtils.createToolBar(actions, ActionUtils.ActionTextBehavior.HIDE);
            toolBar.setBackground(Background.EMPTY);
            toolBar.setVisible(false);
            FxHelper.adjustButtonWidth(toolBar.getItems().stream(), ICON_SIZE_PROFILE * 1.8);

            toolBar.getItems().stream().filter((item) -> (item instanceof ButtonBase))
                    .map((item) -> (ButtonBase) item).forEachOrdered((buttonBase) -> {
                FxHelper.undecorateButton(buttonBase);
            });

            BorderPane.setAlignment(toolBar, Pos.CENTER);

            mBorderPane.setCenter(mainBox);
            BorderPane.setMargin(mainBox, new Insets(8));
            mBorderPane.setRight(toolBar);
            mFadeInTransition.setNode(toolBar);
            mFadeOutTransition.setNode(toolBar);

            mBorderPane.setOnMouseEntered((MouseEvent event) -> {
                if (!toolBar.isVisible()) {
                    toolBar.setVisible(true);
                }

                selectListItem();
                mPreviewPanel.load(getSelectedProfile());
                mFadeInTransition.playFromStart();
            });

            mBorderPane.setOnMouseExited((MouseEvent event) -> {
                mFadeOutTransition.playFromStart();
            });
        }

        private Profile getSelectedProfile() {
            return mListView.getSelectionModel().getSelectedItem();
        }

        private void profileClone() {
            Profile p = getSelectedProfile().clone();
            p.setName(null);
            profileEdit(p);
        }

        private void selectListItem() {
            mListView.getSelectionModel().select(this.getIndex());
            mListView.requestFocus();
        }

        @Override
        protected void updateItem(Profile profile, boolean empty) {
            super.updateItem(profile, empty);

            if (profile == null || empty) {
                clearContent();
            } else {
                addContent(profile);
            }
        }
    }

}
