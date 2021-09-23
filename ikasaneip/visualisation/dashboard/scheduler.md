![IKASAN](../../developer/docs/quickstart-images/Ikasan-title-transparent.png)

# Scheduler Agents
Version 3.2.0 of Ikasan has seen the introduction of a new bundled fully fledged Ikasan Module that fulfils the role of an enterprise scheduler. This
is the first zero code offering of the Ikasan platform. 

All scheduler agents are managed from the Ikasan dashboard as seen below. In a typical deployment a single Scheduler Agent modules is deployed on a 
per host basis. The Scheduler Agent advertises itself to the Ikasan Dashboard when initially started and from this point onwards the agent becomes 
manageable from the dashboard.

The diagram below provides a conceptual view of a single agent per host. 
![Scheduler Conceptual](../../developer/docs/quickstart-images/scheduler.png)

Each agent is responsible for managing any number of scheduled jobs as seen below. 
![Scheduler Agent Conceptual](../../developer/docs/quickstart-images/scheduler-agent-conceptual.png)

### Creating a New Scheduled Job
Upon navigating to the scheduler view, users are presented with the Scheduler Dashboard. The scheduler dashboard provides a view onto all Scheduler Agents as well as the status of all running jobs. In order to manage Scheduler Agent, the user must double click on the table row for the desired Scheduler Agent.
![Search Fields](../../developer/docs/quickstart-images/scheduler-dashboard.png)

The user is presented with the Scheduler Agent Management Dialog. This dialog contains details of the Scheduled Agent along with a list of jobs associated with the agent. The status of the available along with variious controls relating to the job. In the top right corner of the dialog is a plus icon that is clicked in order to create a new job. 
![Wiretap View](../../developer/docs/quickstart-images/scheduler-agent-management-view.png)


![Wiretap Search](../../developer/docs/quickstart-images/scheduler-agents-widget.png)

### Modules and Agents Widget
![Wiretap View](../../developer/docs/quickstart-images/scheduler-agents-status-widget.png)


### Hospital Events Widget
![Wiretap View](../../developer/docs/quickstart-images/scheduled-job-controls.png)

![Wiretap View](../../developer/docs/quickstart-images/scheduled-job-configuration-dialog.png)

### Error Occurrences Widget
![Wiretap View](../../developer/docs/quickstart-images/scheduled-jobs-tab.png)

![Wiretap View](../../developer/docs/quickstart-images/scheduled-job-execution-details-dialog.png)

![Wiretap View](../../developer/docs/quickstart-images/scheduled-job-statistics-dialog.png)

