function [onset, output] = fixation(system, t, varargin)

% show the background
[screenWidth, screenHeight] = Screen('WindowSize', system.w);

color = [255 255 255];

drawScreen(system, t, color, varargin)

% Set the font size to make the '+' bigger
Screen('TextSize', system.w, 75); 

% draw fixation cross
DrawFormattedText(system.w, '+' ,system.pos.up.x,screenHeight/2 + (screenHeight/4), t.cross_color);


progressBar(system, t)

if t.i < 3
    correctTrials = varargin{1}(t.i-3).output.score;

else

    correctTrials = varargin{1}(t.i-1).output.correctTrials;

end

totalCount(system, correctTrials);
coinPile(system, correctTrials)


%onset = t.onset + system.starttime; % abs time
ideal = t.onset + GetSecs(); % relative time
onset = Screen('Flip', system.w, ideal);
fprintf('fixation %f %f %f\n',ideal, onset, onset-ideal)
output.onset_ideal = ideal;
output.onset = onset;

end
