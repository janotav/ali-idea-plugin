package com.hp.alm.ali.idea.action.task;

import com.hp.alm.ali.idea.action.EntityAction;
import com.hp.alm.ali.idea.entity.EntityEditManager;
import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.ui.editor.TaskAddInvestedEditor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;

import java.util.Collections;
import java.util.Set;

public class TaskAddInvestedAction extends EntityAction {

    public TaskAddInvestedAction() {
        super("Add invested", "Add time invested and decrease remaining time", IconLoader.getIcon("/actions/profile.png"));
    }

    @Override
    protected Set<String> getSupportedEntityTypes() {
        return Collections.singleton("project-task");
    }

    @Override
    public void update(AnActionEvent event, Project project, Entity entity) {
        EntityEditManager entityEditManager = project.getComponent(EntityEditManager.class);
        event.getPresentation().setEnabled(!entityEditManager.isEditing(entity));
    }

    @Override
    protected void actionPerformed(AnActionEvent event, Project project, Entity entity) {
        TaskAddInvestedEditor taskAddInvestedEditor = new TaskAddInvestedEditor(project, entity);
        taskAddInvestedEditor.execute();
    }
}
