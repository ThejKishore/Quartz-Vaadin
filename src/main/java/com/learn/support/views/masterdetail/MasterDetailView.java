package com.learn.support.views.masterdetail;

import com.learn.support.data.entity.SamplePerson;
import com.learn.support.data.service.SamplePersonService;
import com.learn.support.quartz.entity.SchedulerJobInfo;
import com.learn.support.quartz.service.SchedulerJobService;
import com.learn.support.views.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dependency.Uses;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

@PageTitle("Master-Detail")
@Route(value = "master-detail/:scheduleJobId?/:action?(edit)", layout = MainLayout.class)
@Uses(Icon.class)
public class MasterDetailView extends Div implements BeforeEnterObserver {

    private final String SCHEDULE_JOB_ID = "scheduleJobId";
    private final String SCHDEULE_JOB_EDIT_ROUTE_TEMPLATE = "master-detail/%s/edit";

    private final Grid<SchedulerJobInfo> grid = new Grid<>(SchedulerJobInfo.class, false);

    private TextField jobName;
    private TextField jobGroup;
    private TextField jobStatus;
    private TextField jobClass;
    private TextField cronExpression;
    private TextField desc;
    private TextField interfaceName;
    private TextField repeatTime;
    private Checkbox cronJob;

    private final Button cancel = new Button("Cancel");
    private final Button save = new Button("Save");

    private final BeanValidationBinder<SchedulerJobInfo> binder;

    private SchedulerJobInfo schedulerJobInfo;

    private final SchedulerJobService schedulerJobService;

    @Autowired
    public MasterDetailView(SchedulerJobService schedulerJobService) {
        this.schedulerJobService = schedulerJobService;
        addClassNames("master-detail-view");

        // Create UI
        SplitLayout splitLayout = new SplitLayout();

        createGridLayout(splitLayout);
        createEditorLayout(splitLayout);

        add(splitLayout);

        // Configure Grid
        grid.addColumn("jobId").setAutoWidth(true);
        grid.addColumn("jobName").setAutoWidth(true);
        grid.addColumn("jobGroup").setAutoWidth(true);
        grid.addColumn("jobStatus").setAutoWidth(true);
        grid.addColumn("jobClass").setAutoWidth(true);
        grid.addColumn("cronExpression").setAutoWidth(true);
        grid.addColumn("desc").setAutoWidth(true);
        grid.addColumn("interfaceName").setAutoWidth(true);
        grid.addColumn("repeatTime").setAutoWidth(true);
        grid.addColumn("cronJob").setAutoWidth(true);

        GridContextMenu<SchedulerJobInfo> menu = grid.addContextMenu();
        menu.addItem("Start/Schedule", event -> {
            if(event.getItem().map(t-> this.schedulerJobService.startJobNow(t)).orElseGet(() -> false)){
                Notification.show("Job Started or Scheduled");
                refreshGrid();
            }
        });
        menu.addItem("Pause", event -> {
            if(event.getItem().map(t-> this.schedulerJobService.pauseJob(t)).orElseGet(() -> false)){
                Notification.show("Job Paused");
                refreshGrid();
            }
        });
        menu.addItem("Resume", event -> {
            if(event.getItem().map(t-> this.schedulerJobService.resumeJob(t)).orElseGet(() -> false)){
                Notification.show("Job Resumed");
                refreshGrid();
            }
        });
        menu.addItem("Delete", event -> {
            if(event.getItem().map(t-> this.schedulerJobService.deleteJob(t)).orElseGet(() -> false)){
                Notification.show("Job Deleted");
                refreshGrid();
            }
        });

        grid.setItems(query -> schedulerJobService.list(
                PageRequest.of(query.getPage(), query.getPageSize(), VaadinSpringDataHelpers.toSpringDataSort(query)))
                .stream());
        grid.addThemeVariants(GridVariant.LUMO_NO_BORDER);

        // when a row is selected or deselected, populate form
        grid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                UI.getCurrent().navigate(String.format(SCHDEULE_JOB_EDIT_ROUTE_TEMPLATE, event.getValue().getJobId()));
            } else {
                clearForm();
                UI.getCurrent().navigate(MasterDetailView.class);
            }
        });

        // Configure Form
        binder = new BeanValidationBinder<>(SchedulerJobInfo.class);

        // Bind fields. This is where you'd define e.g. validation rules

        binder.bindInstanceFields(this);

        cancel.addClickListener(e -> {
            clearForm();
            refreshGrid();
        });

        save.addClickListener(e -> {
            try {
                if (this.schedulerJobInfo == null) {
                    this.schedulerJobInfo = new SchedulerJobInfo();
                }
                binder.writeBean(this.schedulerJobInfo);
                this.schedulerJobService.saveOrupdate(this.schedulerJobInfo);
                clearForm();
                refreshGrid();
                Notification.show("Schedule Job details stored.");
                UI.getCurrent().navigate(MasterDetailView.class);
            } catch (ValidationException validationException) {
                Notification.show("An exception happened while trying to store the Schedule Job details.");
            } catch (Exception ex) {
//                throw new RuntimeException(ex);
                Notification.show("An exception happened while trying to store the Schedule Job details.");
            }
        });

    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<UUID> samplePersonId = event.getRouteParameters().get(SCHEDULE_JOB_ID).map(UUID::fromString);
        if (samplePersonId.isPresent()) {
            Optional<SchedulerJobInfo> samplePersonFromBackend = this.schedulerJobService.get(samplePersonId.get());
            if (samplePersonFromBackend.isPresent()) {
                populateForm(samplePersonFromBackend.get());
            } else {
                Notification.show(
                        String.format("The requested samplePerson was not found, ID = %s", samplePersonId.get()), 3000,
                        Notification.Position.BOTTOM_START);
                // when a row is selected but the data is no longer available,
                // refresh grid
                refreshGrid();
                event.forwardTo(MasterDetailView.class);
            }
        }
    }

    private void createEditorLayout(SplitLayout splitLayout) {
        Div editorLayoutDiv = new Div();
        editorLayoutDiv.setClassName("editor-layout");

        Div editorDiv = new Div();
        editorDiv.setClassName("editor");
        editorLayoutDiv.add(editorDiv);

        FormLayout formLayout = new FormLayout();
        jobName = new TextField("Job Name");
        jobGroup = new TextField("Job Group");
        jobStatus = new TextField("Job Status");
        cronExpression = new TextField("Cron Expression");
        desc = new TextField("Desc");
        interfaceName = new TextField("Interface Name");
        repeatTime = new TextField("Repeat Time");
        cronJob = new Checkbox("Cron Job");



        formLayout.add(jobName,jobGroup,jobStatus,cronExpression,desc,interfaceName,repeatTime,cronJob);

        editorDiv.add(formLayout);
        createButtonLayout(editorLayoutDiv);

        splitLayout.addToSecondary(editorLayoutDiv);
    }

    private void createButtonLayout(Div editorLayoutDiv) {
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setClassName("button-layout");
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        buttonLayout.add(save, cancel);
        editorLayoutDiv.add(buttonLayout);
    }

    private void createGridLayout(SplitLayout splitLayout) {
        Div wrapper = new Div();
        wrapper.setClassName("grid-wrapper");
        splitLayout.addToPrimary(wrapper);
        wrapper.add(grid);
    }

    private void refreshGrid() {
        grid.select(null);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        populateForm(null);
    }

    private void populateForm(SchedulerJobInfo value) {
        this.schedulerJobInfo = value;
        binder.readBean(this.schedulerJobInfo);

    }
}
