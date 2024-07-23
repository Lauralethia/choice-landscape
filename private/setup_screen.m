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

function w = testscreen()
  bg = [254 227 180];
  screen = 0;
  res = [0 0 800 600];
  Screen('Preference', 'SkipSyncTests', 2);
  w = Screen('OpenWindow', screen, bg, res);
end
