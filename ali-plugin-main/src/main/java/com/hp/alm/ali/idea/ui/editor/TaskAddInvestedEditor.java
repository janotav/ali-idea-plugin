package com.hp.alm.ali.idea.ui.editor;

import com.hp.alm.ali.idea.content.taskboard.TaskBoardFlow;
import com.hp.alm.ali.idea.content.taskboard.TaskPanel;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.ui.editor.field.SpinnerField;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.BorderLayout;

public class TaskAddInvestedEditor extends BaseEditor implements BaseEditor.SaveHandler {

    final static String SUBTRACT_MESSAGE = getInfoMessage("Add selected amount to invested time and removes same<br> amount from remaining time.<br/>If there is not enough remaining time it will be set to 0.");
    final static String COMPLETE_MESSAGE = getInfoMessage("Add selected amount to invested time and mark the<br> task as completed.");

    private EntityService entityService;
    private TaskBoardFlow taskBoardFlow;
    private JCheckBox completed;

    public TaskAddInvestedEditor(Project project, Entity task) {
        super(project, "Add Invested Time", task, null);

        saveHandler = this;
        entityService = project.getComponent(EntityService.class);
        taskBoardFlow = project.getComponent(TaskBoardFlow.class);
    }

    @Override
    public void update() {
        addField("invested", new SpinnerField("Invested", "0", true));

        final JTextPane addProperty = new JTextPane();
        addProperty.setBackground(gridFooter.getBackground());
        HTMLEditorKit htmlEditorKit = new HTMLEditorKit();
        addProperty.setEditorKit(htmlEditorKit);
        addProperty.setText(SUBTRACT_MESSAGE);
        addProperty.setEditable(false);
        final JPanel jPanel = new JPanel(new BorderLayout());
        completed = new JCheckBox("Mark completed", false);
        completed.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (completed.isSelected()) {
                    addProperty.setPreferredSize(addProperty.getPreferredSize());
                    addProperty.setText(COMPLETE_MESSAGE);
                } else {
                    addProperty.setText(SUBTRACT_MESSAGE);
                }
            }
        });
        jPanel.add(completed, BorderLayout.NORTH);
        jPanel.add(addProperty, BorderLayout.CENTER);
        gridFooter.add(jPanel);

        packAndPosition();
    }

    private static String getInfoMessage(String htmlFragment) {
        return "<html><body><i style=\"font-size:small;\"><b>Info:</b><br/>" + htmlFragment + "</i></body></html>";
    }

    @Override
    public boolean save(Entity modified, Entity base) {
        modified.setProperty("release-backlog-item-id", entity.getPropertyValue("release-backlog-item-id"));

        int addInvested = Integer.valueOf(modified.getPropertyValue("invested"));
        int invested = Integer.valueOf(entity.getPropertyValue("invested")) + addInvested;
        modified.setProperty("invested", String.valueOf(invested));
        if (!completed.isSelected()) {
            int remaining = Integer.valueOf(entity.getPropertyValue("remaining")) - addInvested;
            modified.setProperty("remaining", remaining > 0 ? String.valueOf(remaining) : "0");
        } else {
            modified.setProperty("remaining", String.valueOf(0));
            modified.setProperty("status", TaskPanel.TASK_COMPLETED);
        }

        final Entity updatedTask = entityService.updateEntity(modified, null, false);
        if(updatedTask != null) {
            taskBoardFlow.onTaskUpdated(updatedTask);
            return true;
        } else {
            return false;
        }
    }
}
