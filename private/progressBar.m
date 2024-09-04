function [] = progressBar(system, t)

timing = load_events();
totalTrials = length(timing);
%function for progress bar

[screenWidth, screenHeight] = Screen('WindowSize', system.w);

% Set up progress bar parameters
progressBarHeight = 20;
progressBarWidth = screenWidth * 0.5; % 50% of screen width
progressBarXpos = screenWidth * 0.25;  % 25% from left edge
progressBarYpos = screenHeight - 50;   % 50 pixels from bottom

% Task parameters
currentTrial = t.i;

% Define the full white progress bar rectangle
progressBarFullRect = [progressBarXpos, progressBarYpos, progressBarXpos + progressBarWidth, progressBarYpos + progressBarHeight];

% Compute progress
progress = currentTrial / totalTrials;
currentBarWidth = progressBarWidth * progress;

% Define the green progress bar rectangle
progressBarGreenRect = [progressBarXpos, progressBarYpos, progressBarXpos + currentBarWidth, progressBarYpos + progressBarHeight];

% Draw the full white progress bar
Screen('FillRect', system.w, [255 255 255], progressBarFullRect);

% Draw the green progress bar overlay
Screen('FillRect', system.w, [0 100 0], progressBarGreenRect);
