package com.hp.alm.ali.idea.ui.editor;

import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.ui.editor.field.SpinnerField;
import com.intellij.openapi.project.Project;

import java.awt.*;

public class TaskAddInvestedEditor extends BaseEditor {

    protected EntityService entityService;

    public TaskAddInvestedEditor(Project project, Entity task) {
        this(project, task, new Edit(project));
    }

    public TaskAddInvestedEditor(Project project, Entity task, SaveHandler saveHandler) {
        super(project, "Add invested time", task, saveHandler);

        entityService = project.getComponent(EntityService.class);

        setSize(new Dimension(230, 150));
        centerOnOwner();
    }

    @Override
    public void update() {
        addField("invested", new SpinnerField("Add invested", "0", true));
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
