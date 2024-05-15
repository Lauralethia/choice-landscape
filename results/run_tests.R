#!/usr/bin/env Rscript

# run all test_* functions
# test functions are kept next to the actual functions they test.
# this is not idomatic R, where we'd want tests/test_*.R (eg. from `usethis::use_test("habit")`)
# so we need our own wrapper instead of testthat::run_test()
#
# 20231210WF - init

source('read_raw.R')
#lapply(grep('test_',ls(),value=T),do.call,args=list())
my_test_funcs <- grep('test_',ls(),value=T)
my_test_funcs <- Filter(function(fname) class(get(fname))=="function", my_test_funcs)

invisible(lapply(my_test_funcs,
       function(tname)
          testthat::test_that(tname, {
             do.call(tname,args=list())
          })))
