#!/usr/bin/env Rscript
# run like
# Rscript -e "shiny::runApp()"
source('habit_plots.R') # reads rawdata, sets MAXTRIALS
library(shiny)

MINDATE <-min(rawdata$vdate)
MAXDATE <-max(rawdata$vdate)
TASKS <- unique(rawdata$task)
VERS <- unique(rawdata$ver)
ui <- fluidPage(
    titlePanel("habitTask"),
    sidebarLayout(
     sidebarPanel(
      sliderInput("date_range", label="dates", min=MINDATE, max=MAXDATE,
                  value=c(ymd_hms("2022-03-10T00:00:00"),MAXDATE)),
      selectInput("task_selection", label="tasks (each mturk is new task; shift click for multiple)",
                  choices=TASKS, selected=TASKS, multiple = TRUE),
      #selectInput("versions", label="versions of task (task code)",
      #            choices=VERS, selected=VERS, multiple = TRUE),
     ), mainPanel(
         tabsetPanel(type="tabs",
           tabPanel("runs tbl", tableOutput("smry_tbl")),
           tabPanel("habit plot", plotOutput("habit_line")),
           tabPanel("grp learn", plotOutput("plot_grp_learn_trace")),
           tabPanel("grp far", plotOutput("plot_grp_far_trace")),
           tabPanel("grp rt", plotOutput("plot_grp_rt_trace_mvavg")),
           tabPanel("choice tbl", tableOutput("pchoice_tbl")),
           tabPanel("learn optimal", plotOutput("plot_learn_optimal")),
           tabPanel("pref far", plotOutput("plot_pref_far")),
           tabPanel("rev learn", plotOutput("plot_revlearn")),
))))

server <- function(input, output){
    d <- reactive({
        subset_data(rawdata,
                    date_range=input$date_range,
                    task_selection=input$task_selection,
                    versions = VERS
                    #versions=input$versions
                    )})
    output$habit_line <- renderPlot({plot_habit_line(d())})
    output$plot_grp_learn_trace <- renderPlot({plot_grp_learn(d(),trace=TRUE)})
    output$plot_grp_far_trace <- renderPlot({plot_grp_far_trace(d())})
    output$plot_grp_rt_trace_mvavg <- renderPlot({plot_grp_rt_trace_mvavg(d())})
     

    output$smry_tbl <- renderTable({all_runs(d())})
    output$pchoice_tbl <-renderTable({smry_pChoice(d())})
    
    output$plot_learn_optimal <- renderPlot({plot_learn_optimal(d())})
    output$plot_pref_far <- renderPlot({plot_pref_far(d())})
    output$plot_revlearn <- renderPlot({plot_revlearn(d())})


    return(output)
}

# options(browser="firefox")
shinyApp(ui, server)
