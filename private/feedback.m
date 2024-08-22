function [onset, output] = feedback(system, t, record)
ideal = t.onset + GetSecs(); % relative time

Screen('DrawTexture', system.w, system.tex.ocean_bottom);
% reach back in time and find the previous choice event
choice = record(t.i-2).output,
if choice.score
    msg = 'REWARD!';
    DrawFormattedText(system.w, msg ,...
        'center','center', [255,255,255]);

    % Load an audio file
    [cash, cashFs] = audioread('out/audio/cash.mp3');

    % Play the audio
    sound(cash, cashFs);
else
    msg = 'NO REWARD!';
    DrawFormattedText(system.w, msg ,...
        'center','center', [255,255,255]);

    % Load an audio file
    [buzz, buzzFs] = audioread('out/audio/buzzer.mp3');

    % Play the audio
    sound(buzz, buzzFs);
end

% TODO: find chosen direction using choice.key
% TODO: show open/closed chest over choice location
% TODO: animate chest sprite

onset = Screen('Flip', system.w, ideal);
output.ideal = ideal;
end
