function background(system)

screenWidth = 800; 
imageWidth = 558;
padding = (screenWidth - imageWidth)/2;

Screen('DrawTexture', system.w, system.tex.ocean_border, ...
        [], [0, 0, screenWidth, 299] );

Screen('DrawTexture', system.w, system.tex.ocean_bottom, ...
        [], [padding, 0, 558+padding 299] );


end