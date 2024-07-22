function closedown()
% CLOSEDOWN - clean up PTB connections
% originally from https://github.com/LabNeuroCogDevel/froggerRPG/blob/master/private/closedown.m
    ListenChar(0);
    ShowCursor;
    Screen('CloseAll');
    Priority(0);
    sca;
    diary off;
    close all;
    %openPTBSnd('close');
    % TODO: close DAQ?
end
