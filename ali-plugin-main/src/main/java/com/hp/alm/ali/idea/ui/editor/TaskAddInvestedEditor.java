package com.hp.alm.ali.idea.ui.editor;

import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.ui.editor.field.SpinnerField;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;

public class TaskAddInvestedEditor extends BaseEditor {

    protected EntityService entityService;

    public TaskAddInvestedEditor(Project project, Entity task) {
        this(project, task, new Edit(project));
    }

    public TaskAddInvestedEditor(Project project, Entity task, SaveHandler saveHandler) {
        super(project, "Add Invested Time", task, saveHandler);

        entityService = project.getComponent(EntityService.class);
    }

    @Override
    public void update() {
        addField("invested", new SpinnerField("Invested", "0", true));

        JTextPane addProperty = new JTextPane();
        addProperty.setBackground(gridFooter.getBackground());
        addProperty.setEditorKit(new HTMLEditorKit());
        addProperty.setText("<html><body><i style=\"font-size:x-small;\"><b>Info:</b><br/>Add selected amount to invested time and removes same<br> amount from remaining time.<br/>If there is not enough remaining time it will be set to 0.</i></body></html>");
        addProperty.setEditable(false);
        gridFooter.add(addProperty);

        packAndPosition();
    }

    public static class Edit implements SaveHandler {

        private EntityService entityService;

        public Edit(Project project) {
            entityService = project.getComponent(EntityService.class);
        }

        @Override
        public boolean save(Entity modified, Entity entity) {
            modified.setProperty("release-backlog-item-id", entity.getPropertyValue("release-backlog-item-id"));

            int addInvested = Integer.valueOf(modified.getPropertyValue("invested"));
            int invested = Integer.valueOf(entity.getPropertyValue("invested")) + addInvested;
            int remaining = Integer.valueOf(entity.getPropertyValue("remaining")) - addInvested;
            modified.setProperty("invested", String.valueOf(invested));
            modified.setProperty("remaining", remaining > 0 ? String.valueOf(remaining) : "0");

            final Entity updatedTask = entityService.updateEntity(modified, null, false);
            if(updatedTask != null) {
                entityService.getEntityAsync(new EntityRef("release-backlog-item", Integer.valueOf(entity.getPropertyValue("release-backlog-item-id"))), null);
                return true;
            } else {
                return false;
            }
        }
    }

}
