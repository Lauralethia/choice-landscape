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
      sliderInput("date_range", label="dates", min=MINDATE, max=MAXDATE, value=c(MINDATE,MAXDATE)),
      selectInput("task_selection", label="tasks (each mturk is new task)",
                  choices=TASKS, selected=TASKS, multiple = TRUE),
      selectInput("versions", label="tasks (each mturk is new task)",
                  choices=VERS, selected=VERS, multiple = TRUE),
     ), mainPanel(
         tabsetPanel(type="tabs",
           tabPanel("habit plot", plotOutput("habit_line")),
           tabPanel("runs tbl", tableOutput("smry_tbl")),
           tabPanel("choice tbl", tableOutput("pchoice_tbl")),
           tabPanel("learn optimal", plotOutput("plot_learn_optimal")),
           tabPanel("pref far", plotOutput("plot_pref_far")),
           tabPanel("rev learn", plotOutput("plot_revlearn")),
           tabPanel("grp learn", plotOutput("plot_grp_learn")),
           tabPanel("grp far", plotOutput("plot_grp_far")),
))))

server <- function(input, output){
    d <- reactive({
        subset_data(rawdata, input$date_range,
                    # todo input$tasks_selections
                    #      input$versions
                    tasks_selection=unique(rawdata$task),
                  versions=unique(rawdata$ver))})

    output$smry_tbl <- renderTable({all_runs(d())})
    output$pchoice_tbl <-renderTable({smry_pChoice(d())})
    
    output$habit_line <- renderPlot({plot_habit_line(d())})
    output$plot_learn_optimal <- renderPlot({plot_lean_optimal(d())})
    output$plot_pref_far <- renderPlot({plot_pref_far(d())})
    output$plot_revlearn <- renderPlot({plot_revlearn(d())})
    output$plot_grp_learn <- renderPlot({plot_grp_learn(d())})
    output$plot_grp_far <- renderPlot({plot_grp_far(d())})


    return(output)
}

# options(browser="firefox")
shinyApp(ui, server)
