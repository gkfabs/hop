/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hop.workflow.actions.pipeline;

import org.apache.hop.core.Const;
import org.apache.hop.core.exception.HopException;
import org.apache.hop.core.extension.ExtensionPointHandler;
import org.apache.hop.core.extension.HopExtensionPoint;
import org.apache.hop.core.logging.LogChannel;
import org.apache.hop.core.logging.LogLevel;
import org.apache.hop.core.util.Utils;
import org.apache.hop.core.variables.IVariables;
import org.apache.hop.i18n.BaseMessages;
import org.apache.hop.pipeline.PipelineMeta;
import org.apache.hop.pipeline.config.PipelineRunConfiguration;
import org.apache.hop.ui.core.ConstUi;
import org.apache.hop.ui.core.dialog.BaseDialog;
import org.apache.hop.ui.core.dialog.ErrorDialog;
import org.apache.hop.ui.core.gui.WindowProperty;
import org.apache.hop.ui.hopgui.HopGui;
import org.apache.hop.ui.hopgui.file.pipeline.HopPipelineFileType;
import org.apache.hop.ui.util.SwtSvgImageUtil;
import org.apache.hop.ui.workflow.actions.ActionBaseDialog;
import org.apache.hop.ui.workflow.dialog.WorkflowDialog;
import org.apache.hop.workflow.WorkflowMeta;
import org.apache.hop.workflow.action.ActionBase;
import org.apache.hop.workflow.action.IAction;
import org.apache.hop.workflow.action.IActionDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;

import java.util.List;

/** This dialog allows you to edit the pipeline action (ActionPipeline) */
public class ActionPipelineDialog extends ActionBaseDialog implements IActionDialog {
  private static final Class<?> PKG = ActionPipeline.class; // For Translator

  protected ActionPipeline action;

  private static final String[] FILE_FILTERLOGNAMES =
      new String[] {
        BaseMessages.getString(PKG, "ActionPipeline.Fileformat.TXT"),
        BaseMessages.getString(PKG, "ActionPipeline.Fileformat.LOG"),
        BaseMessages.getString(PKG, "ActionPipeline.Fileformat.All")
      };

  public ActionPipelineDialog(
      Shell parent, IAction action, WorkflowMeta workflowMeta, IVariables variables) {
    super(parent, action, workflowMeta, variables);
    this.action = (ActionPipeline) action;
  }

  @Override
  public IAction open() {
    Shell parent = getParent();
    display = parent.getDisplay();

    shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.MIN | SWT.MAX | SWT.RESIZE);
    props.setLook(shell);
    WorkflowDialog.setShellImage(shell, action);

    backupChanged = action.hasChanged();

    createElements();

    getData();
    setActive();

    BaseDialog.defaultShellHandling(shell, c -> ok(), c -> cancel());

    return action;
  }

  @Override
  protected void createElements() {
    super.createElements();
    shell.setText(BaseMessages.getString(PKG, "ActionPipeline.Header"));

    wlPath.setText(BaseMessages.getString(PKG, "ActionPipeline.PipelineFile.Label"));
    wPassParams.setText(BaseMessages.getString(PKG, "ActionPipeline.PassAllParameters.Label"));

    wClearRows = new Button(gExecution, SWT.CHECK);
    props.setLook(wClearRows);
    wClearRows.setText(BaseMessages.getString(PKG, "ActionPipeline.ClearResultList.Label"));
    FormData fdbClearRows = new FormData();
    fdbClearRows.left = new FormAttachment(0, 0);
    fdbClearRows.top = new FormAttachment(wEveryRow, 10);
    wClearRows.setLayoutData(fdbClearRows);

    wClearFiles = new Button(gExecution, SWT.CHECK);
    props.setLook(wClearFiles);
    wClearFiles.setText(BaseMessages.getString(PKG, "ActionPipeline.ClearResultFiles.Label"));
    FormData fdbClearFiles = new FormData();
    fdbClearFiles.left = new FormAttachment(0, 0);
    fdbClearFiles.top = new FormAttachment(wClearRows, 10);
    wClearFiles.setLayoutData(fdbClearFiles);

    wWaitingToFinish = new Button(gExecution, SWT.CHECK);
    props.setLook(wWaitingToFinish);
    wWaitingToFinish.setText(BaseMessages.getString(PKG, "ActionPipeline.WaitToFinish.Label"));
    FormData fdWait = new FormData();
    fdWait.top = new FormAttachment(wClearFiles, 10);
    fdWait.left = new FormAttachment(0, 0);
    wWaitingToFinish.setLayoutData(fdWait);

    wbGetParams.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent arg0) {
            getParameters(null); // force reload from file specification
          }
        });

    wbBrowse.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            pickFileVFS();
          }
        });

    wbLogFilename.addSelectionListener(
        new SelectionAdapter() {
          @Override
          public void widgetSelected(SelectionEvent e) {
            selectLogFile(FILE_FILTERLOGNAMES);
          }
        });
  }

  @Override
  protected ActionBase getAction() {
    return action;
  }

  @Override
  protected Image getImage() {
    return SwtSvgImageUtil.getImage(
        shell.getDisplay(),
        getClass().getClassLoader(),
        "ui/images/pipeline.svg",
        ConstUi.LARGE_ICON_SIZE,
        ConstUi.LARGE_ICON_SIZE);
  }

  @Override
  protected String[] getParameters() {
    return action.parameters;
  }

  private void getParameters(PipelineMeta inputPipelineMeta) {
    try {
      if (inputPipelineMeta == null) {
        ActionPipeline jet = new ActionPipeline();
        getInfo(jet);
        inputPipelineMeta = jet.getPipelineMeta(this.getMetadataProvider(), variables);
      }
      String[] parameters = inputPipelineMeta.listParameters();

      String[] existing = wParameters.getItems(1);

      for (int i = 0; i < parameters.length; i++) {
        if (Const.indexOfString(parameters[i], existing) < 0) {
          TableItem item = new TableItem(wParameters.table, SWT.NONE);
          item.setText(1, parameters[i]);
        }
      }
      wParameters.removeEmptyRows();
      wParameters.setRowNums();
      wParameters.optWidth(true);
    } catch (Exception e) {
      new ErrorDialog(
          shell,
          BaseMessages.getString(PKG, "ActionPipelineDialog.Exception.UnableToLoadPipeline.Title"),
          BaseMessages.getString(
              PKG, "ActionPipelineDialog.Exception.UnableToLoadPipeline.Message"),
          e);
    }
  }

  protected void pickFileVFS() {
    HopPipelineFileType<PipelineMeta> pipelineFileType = new HopPipelineFileType<>();
    String filename =
        BaseDialog.presentFileDialog(
            shell,
            wPath,
            variables,
            pipelineFileType.getFilterExtensions(),
            pipelineFileType.getFilterNames(),
            true);
    if (filename != null) {
      replaceNameWithBaseFilename(filename);
    }
  }

  public String getEntryName(String name) {
    return "${" + Const.INTERNAL_VARIABLE_ENTRY_CURRENT_FOLDER + "}/" + name;
  }

  public void dispose() {
    props.setScreen(new WindowProperty(shell));
    shell.dispose();
  }

  public void getData() {
    wName.setText(Const.NVL(action.getName(), ""));
    wPath.setText(Const.NVL(action.getFilename(), ""));

    // Parameters
    if (action.parameters != null) {
      for (int i = 0; i < action.parameters.length; i++) {
        TableItem ti = wParameters.table.getItem(i);
        if (!Utils.isEmpty(action.parameters[i])) {
          ti.setText(1, Const.NVL(action.parameters[i], ""));
          ti.setText(2, Const.NVL(action.parameterFieldNames[i], ""));
          ti.setText(3, Const.NVL(action.parameterValues[i], ""));
        }
      }
      wParameters.setRowNums();
      wParameters.optWidth(true);
    }

    wPassParams.setSelection(action.isPassingAllParameters());

    if (action.logfile != null) {
      wLogfile.setText(action.logfile);
    }
    if (action.logext != null) {
      wLogext.setText(action.logext);
    }

    wPrevToParams.setSelection(action.paramsFromPrevious);
    wEveryRow.setSelection(action.execPerRow);
    wSetLogfile.setSelection(action.setLogfile);
    wAddDate.setSelection(action.addDate);
    wAddTime.setSelection(action.addTime);
    wClearRows.setSelection(action.clearResultRows);
    wClearFiles.setSelection(action.clearResultFiles);
    wWaitingToFinish.setSelection(action.isWaitingToFinish());
    wAppendLogfile.setSelection(action.setAppendLogfile);

    wbLogFilename.setSelection(action.setAppendLogfile);

    wCreateParentFolder.setSelection(action.createParentFolder);
    if (action.logFileLevel != null) {
      wLoglevel.select(action.logFileLevel.getLevel());
    }

    try {
      List<String> runConfigurations =
          this.getMetadataProvider()
              .getSerializer(PipelineRunConfiguration.class)
              .listObjectNames();

      try {
        ExtensionPointHandler.callExtensionPoint(
            HopGui.getInstance().getLog(),
            variables,
            HopExtensionPoint.HopGuiRunConfiguration.id,
            new Object[] {runConfigurations, PipelineMeta.XML_TAG});
      } catch (HopException e) {
        // Ignore errors
      }

      wRunConfiguration.setItems(runConfigurations.toArray(new String[0]));
      wRunConfiguration.setText(Const.NVL(action.getRunConfiguration(), ""));

      if (Utils.isEmpty(action.getRunConfiguration())) {
        wRunConfiguration.select(0);
      } else {
        wRunConfiguration.setText(action.getRunConfiguration());
      }
    } catch (Exception e) {
      LogChannel.UI.logError("Error getting pipeline run configurations", e);
    }

    wName.selectAll();
    wName.setFocus();
  }

  @Override
  protected void cancel() {
    action.setChanged(backupChanged);

    action = null;
    dispose();
  }

  private void getInfo(ActionPipeline actionPipeline) throws HopException {
    actionPipeline.setName(wName.getText());
    actionPipeline.setRunConfiguration(wRunConfiguration.getText());
    actionPipeline.setFileName(wPath.getText());
    if (actionPipeline.getFilename().isEmpty()) {
      throw new HopException(
          BaseMessages.getString(
              PKG, "ActionPipeline.Dialog.Exception.NoValidMappingDetailsFound"));
    }

    // Do the parameters
    int nrItems = wParameters.nrNonEmpty();
    int nr = 0;
    for (int i = 0; i < nrItems; i++) {
      String param = wParameters.getNonEmpty(i).getText(1);
      if (param != null && param.length() != 0) {
        nr++;
      }
    }
    actionPipeline.parameters = new String[nrItems];
    actionPipeline.parameterFieldNames = new String[nrItems];
    actionPipeline.parameterValues = new String[nrItems];
    nr = 0;
    for (int i = 0; i < nrItems; i++) {
      String param = wParameters.getNonEmpty(i).getText(1);
      String fieldName = wParameters.getNonEmpty(i).getText(2);
      String value = wParameters.getNonEmpty(i).getText(3);

      actionPipeline.parameters[nr] = param;

      if (!Utils.isEmpty(Const.trim(fieldName))) {
        actionPipeline.parameterFieldNames[nr] = fieldName;
      } else {
        actionPipeline.parameterFieldNames[nr] = "";
      }

      if (!Utils.isEmpty(Const.trim(value))) {
        actionPipeline.parameterValues[nr] = value;
      } else {
        actionPipeline.parameterValues[nr] = "";
      }

      nr++;
    }

    actionPipeline.setPassingAllParameters(wPassParams.getSelection());

    actionPipeline.logfile = wLogfile.getText();
    actionPipeline.logext = wLogext.getText();

    if (wLoglevel.getSelectionIndex() >= 0) {
      actionPipeline.logFileLevel = LogLevel.values()[wLoglevel.getSelectionIndex()];
    } else {
      actionPipeline.logFileLevel = LogLevel.BASIC;
    }

    actionPipeline.paramsFromPrevious = wPrevToParams.getSelection();
    actionPipeline.execPerRow = wEveryRow.getSelection();
    actionPipeline.setLogfile = wSetLogfile.getSelection();
    actionPipeline.addDate = wAddDate.getSelection();
    actionPipeline.addTime = wAddTime.getSelection();
    actionPipeline.clearResultRows = wClearRows.getSelection();
    actionPipeline.clearResultFiles = wClearFiles.getSelection();
    actionPipeline.createParentFolder = wCreateParentFolder.getSelection();
    actionPipeline.setRunConfiguration(wRunConfiguration.getText());
    actionPipeline.setAppendLogfile = wAppendLogfile.getSelection();
    actionPipeline.setWaitingToFinish(wWaitingToFinish.getSelection());
  }

  @Override
  protected void ok() {
    if (Utils.isEmpty(wName.getText())) {
      MessageBox box = new MessageBox(shell, SWT.OK | SWT.ICON_ERROR);
      box.setText(BaseMessages.getString(PKG, "System.TransformActionNameMissing.Title"));
      box.setMessage(BaseMessages.getString(PKG, "System.ActionNameMissing.Msg"));
      box.open();
      return;
    }
    action.setName(wName.getText());

    try {
      getInfo(action);
    } catch (HopException e) {
      // suppress exceptions at this time - we will let the runtime report on any errors
    }
    action.setChanged();
    dispose();
  }
}
