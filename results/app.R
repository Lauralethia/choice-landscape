#!/usr/bin/env Rscript
# run like
# Rscript -e "shiny::runApp()"
# Rscript -e "shiny::runApp(host='0.0.0.0', port=3001)"

source('habit_plots.R') # reads rawdata, sets MAXTRIALS
library(shiny)

MINDATE <- ymd(min(rawdata$vdate,na.rm=T))
MAXDATE <- ymd(max(rawdata$vdate,na.rm=T))+days(1)
print(glue::glue("#date slider: from {MINDATE} to {MAXDATE}"))
TASKS <- unique(rawdata$task)
BLKSQ <- unique(rawdata$blockseq)
VERS <- unique(rawdata$ver)
ui <- fluidPage(
    titlePanel("habitTask"),
    sidebarLayout(
     sidebarPanel(
      sliderInput("date_range", label="dates", min=MINDATE, max=MAXDATE,
                  value=c(MINDATE,MAXDATE)),
      selectInput("blockseq_select", label="block types",
                  choices=BLKSQ, selected=BLKSQ, multiple = TRUE),
      selectInput("task_selection", label="tasks (each mturk is new task; shift click for multiple)",
                  choices=TASKS, selected=TASKS, multiple = TRUE),
      #selectInput("versions", label="versions of task (task code)",
      #            choices=VERS, selected=VERS, multiple = TRUE),
      actionButton("reload_data", "Reload data"),
      checkboxInput("tasktblgrp","perm tbl w/task?",value=FALSE)
     ), mainPanel(
         tabsetPanel(type="tabs",
           tabPanel("runs tbl", tableOutput("smry_tbl")),
           tabPanel("perm tbl", tableOutput("smry_perms")),
           tabPanel("habit plot", plotOutput("habit_line")),
           tabPanel("idv runs", plotOutput("plot_idv")),
           tabPanel("grp learn", plotOutput("plot_grp_learn_trace")),
           tabPanel("grp far", plotOutput("plot_grp_far_trace")),
           tabPanel("grp rt", plotOutput("plot_grp_rt_trace_mvavg")),
           tabPanel("choice tbl", tableOutput("pchoice_tbl")),
           tabPanel("learn optimal", plotOutput("plot_learn_optimal")),
           tabPanel("pref far", plotOutput("plot_pref_far")),
           tabPanel("rev learn", plotOutput("plot_revlearn")),
           tabPanel("age hist", plotOutput("plot_age_hist")),
           tabPanel("habit hist", plotOutput("plot_habit_hist")),
))))

server <- function(input, output){
    # rerun fetch and update global vars
    observeEvent(input$reload_data,{
      update_data(runMake=TRUE) # update globals raw_data, summary_data
      #NB MAXDATE likely still off
    })

    d <- reactive({
        subset_data(rawdata,
                    date_range=input$date_range,
                    task_selection=input$task_selection,
                    versions = VERS,
                    blockseq_select=input$blockseq_select
                    #versions=input$versions
                    )})
    output$habit_line <- renderPlot({plot_habit_line(d())})
    output$plot_idv <- renderPlot({plot_idv_wf(d())})
    output$plot_grp_learn_trace <- renderPlot({plot_grp_learn(d(),idv_ma_win=3)})
    output$plot_grp_far_trace <- renderPlot({plot_grp_far_trace(d(),idv_ma_win=20)})
    output$plot_grp_rt_trace_mvavg <- renderPlot({plot_grp_rt_trace_mvavg(d())})
     

    output$smry_tbl <- renderTable({all_runs(d()) %>% select(-ver,-task)})
    output$smry_perms <- renderTable({smry_perms(d(), usetask=input$tasktblgrp)})
    output$pchoice_tbl <-renderTable({smry_pChoice(d())})
    
    output$plot_learn_optimal <- renderPlot({plot_learn_optimal(d())})
    output$plot_pref_far <- renderPlot({plot_pref_far(d())})
    output$plot_revlearn <- renderPlot({plot_revlearn(d())})
    output$plot_revlearn <- renderPlot({plot_revlearn(d())})
    output$plot_age_hist<- renderPlot({plot_age_hist(d())})
    output$plot_habit_hist <- renderPlot({plot_habit_hist(d())})

    return(output)
}

# options(browser="firefox")
shinyApp(ui, server)
