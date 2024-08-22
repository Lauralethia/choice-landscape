
function w = setup_screen(varargin)
fprintf('# loading screen\n');

helpdir='/home/abel/matlabTasks/taskHelperFunctions/';
if ~exist(helpdir, 'dir')
    w = testscreen();
    fprintf('WARNING: could not find taskHelperFunctions!')
else
    addpath(helpdir)
    disp('running initializeScreen from taskHerlperFuncions (2nd screen)')
    [w,~] = initializeScreen();
    disp('connected to second screen')
end

% for transprent (alpha channel) png textures
Screen('BlendFunction', w, 'GL_SRC_ALPHA', 'GL_ONE_MINUS_SRC_ALPHA');
end

function w = testscreen() % edited so that it opens on a second screen if there is one and finds the size of that screen - SM
bg = [254 227 180];
screens = Screen('Screens');
screenNumber = max(screens);
rect = Screen('Rect', screenNumber);
Screen('Preference', 'SkipSyncTests', 2);
w = Screen('OpenWindow', screenNumber, bg, rect);
end
