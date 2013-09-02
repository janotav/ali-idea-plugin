/*
 * Copyright 2013 Hewlett-Packard Development Company, L.P
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.alm.ali.idea.tasks;

import com.hp.alm.ali.idea.model.Entity;
import com.hp.alm.ali.idea.entity.EntityQuery;
import com.hp.alm.ali.idea.entity.EntityRef;
import com.hp.alm.ali.idea.rest.RestService;
import com.hp.alm.ali.idea.services.EntityService;
import com.hp.alm.ali.idea.services.FavoritesService;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.tasks.Task;
import com.intellij.tasks.TaskRepository;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;

import javax.swing.event.HyperlinkEvent;
import java.util.LinkedList;
import java.util.List;

@Tag("HP_ALM")
final public class HpAlmRepository extends TaskRepository implements Comparable<HpAlmRepository> {

    private Project project;

    /**
     * Id has two uses. First of all it is forced by the following code from TaskRepositoriesConfigurable:
     *
     *  TaskRepository clone = repository.clone();
     *  assert clone.equals(repository) : repository.getClass().getName();
     *
     * This assertion prevents us from defining equals for identity only. On the other hand when equals is defined as
     * true for all other (HP ALM) repositories, removal operation is not working properly when multiple
     * (HP ALM) repositories are present - the first occurrence is removed instead of the one that was selected.
     * I wonder if this was worth the trouble, but auxiliary id fixes the problem....
     *
     * More importantly id defines order and allows to ignore duplicated repositories when making queries to HP ALM system...
     */
    private long id;

    private TaskConfig defect = new TaskConfig("defect");
    private TaskConfig requirement = new TaskConfig("requirement");

    public HpAlmRepository() {
        super(new HpAlmRepositoryType());
    }

    public HpAlmRepository(String url, long id) {
        super(new HpAlmRepositoryType());
        this.id = id;
        setUrl(url);
    }

    public void setId(long id) {
        this.id = id;
    }

    @Tag("defect")
    public TaskConfig getDefect() {
        return defect;
    }

    public void setDefect(TaskConfig defect) {
        this.defect = defect;
    }

    @Tag("requirement")
    public TaskConfig getRequirement() {
        return requirement;
    }

    public void setRequirement(TaskConfig requirement) {
        this.requirement = requirement;
    }

    @Attribute("id")
    public long getId() {
        return id;
    }

    public String getPresentableName() {
      return "HP ALM";
    }

    public boolean isConfigured() {
      return true;
    }

    public Task[] getIssues(String query, int max, long since) throws Exception {
        if(!_assignProject()) {
            return new Task[0];
        }
        List<HpAlmTask> list = new LinkedList<HpAlmTask>();
        loadTasks(query, defect, "defect", list);
        loadTasks(query, requirement, "requirement", list);
        return list.toArray(new Task[list.size()]);
    }

    private void loadTasks(String query, TaskConfig config, String entityType, List<HpAlmTask> tasks) {
        if(config.isEnabled()) {
            EntityQuery filter = new EntityQuery(entityType);

            filter.addColumn(HpAlmTask.getDescriptionField(entityType), 1);
            filter.addColumn("name", 1);

            if(config.isCustomSelected()) {
                filter.copyFrom(config.getCustomFilter());
            } else {
                EntityQuery stored = project.getComponent(FavoritesService.class).getStoredQuery(entityType, config.getStoredQuery());
                if(stored != null) {
                    filter.copyFrom(stored);
                } else {
                    Notifications.Bus.notify(new Notification("HP ALM Integration", "Cannot retrieve task information from HP ALM:<br/> '"+config.getStoredQuery()+"' query not found",
                            "<p><a href=\"\">Configure task integration ...</a></p>", NotificationType.ERROR,
                        new NotificationListener() {
                            public void hyperlinkUpdate(Notification notification, HyperlinkEvent event) {
                                notification.expire();
                                ShowSettingsUtil.getInstance().showSettingsDialog(project, "Servers");
                            }
                        }
                    ), project);
                    return;
                }
            }
            filter.setValue("name", "'*"+query+"*'"); // TODO: what if name is already specified in the filter?
            EntityService entityService = project.getComponent(EntityService.class);
            for(Entity entity: entityService.query(filter)) {
                tasks.add(new HpAlmTask(project, entity));
            }
            try {
                int id = Integer.parseInt(query);
                Entity entity = project.getComponent(EntityService.class).getEntity(new EntityRef(entityType, id));
                tasks.add(new HpAlmTask(project, entity));
            } catch(Exception e) {
                // not a number or entity doesn't exist
            }
        }
    }

    public Task findTask(String taskName) throws Exception {
        if(!_assignProject()) {
            return null;
        }
        Entity entity = project.getComponent(EntityService.class).getEntity(new EntityRef(taskName));
        return new HpAlmTask(project, entity);
    }

    public String extractId(String taskName) {
        return taskName;
    }

    public HpAlmRepository clone() {
        HpAlmRepository ret = new HpAlmRepository(getUrl(), id);
        ret.requirement = requirement;
        ret.defect = defect;
        return ret;
    }

    public boolean equals(Object o) {
        if(o == this) {
            return true;
        } else if(!(o instanceof HpAlmRepository)) {
            return false;
        } else if(((HpAlmRepository)o).id != id) {
            return false;
        } else if(getUrl() == null) {
            return ((HpAlmRepository) o).getUrl() == null;
        } else {
            return getUrl().equals(((HpAlmRepository) o).getUrl());
        }
    }

    public int hashCode() {
        return getUrl() == null? 0: getUrl().hashCode() + 31 * new Long(id).hashCode();
    }

    synchronized boolean _assignProject() throws InterruptedException {
        if(project == null) {
            for(Project project: ProjectManager.getInstance().getOpenProjects()) {
                if(project.getName().equals(getUrl())) {
                    this.project = project;
                    break;
                }
            }
        }
        return project != null && project.getComponent(RestService.class).getServerType().isConnected();
    }

    public int compareTo(HpAlmRepository other) {
        if(id < other.id) {
            return -1;
        } else if(id > other.id) {
            return 1;
        } else {
            return 0;
        }
    }
}
