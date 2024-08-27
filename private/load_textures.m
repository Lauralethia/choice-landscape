function [tex_struct] = load_textures(w, varargin)
% read images into struct of named textures tex_struct.ocean_bottom == 32
fprintf('# loading textures\n');
imgs = [dir('out/imgs/*.png')];


avatar_sprites = {'astronaut', 'shark','lizard_blue', 'alient_green'};
well_sprites = {'chest_sprites', 'wellcoin_sprites'};
for f = imgs'
    [~, fname, ~]  = fileparts(f.name); fname(fname=='-')='_';
    [img, ~, alpha] = imread(fullfile(f.folder,f.name));

    % Check if the filename is "ocean_bottom"
    if strcmp(fname, "ocean_bottom")

        screens = Screen('Screens');
        screenNumber = max(screens);

        % Get the size of the screen
        [screenWidth, screenHeight] = Screen('WindowSize', screenNumber);

        % Resize the image to fit the screen size
        img = imresize(img, [screenHeight, screenWidth]);

        if size(img,2) >= 2
            img(:,:,4) = img(:,:,3);

        end


    end

    if size(img,2) >= 2 && ~strcmp(fname, "ocean_bottom")
        img(:,:,4) = alpha;
    end
    % TODO: scale images to screen size

    % sprites within one file. have animations frames in a grid
    if ismember(fname, avatar_sprites)
        % 4 rows 3 cols
        sprites = cell(4,4);
        [x,y,z] =size(img); % z was missing, added 20240723 SDM
        h= floor(x/4);
        wd = floor(y/4);
        for i=1:4
            for j=1:4
                 ii=(i-1)*wd+1; 
                 jj=(j-1)*h+1;
                img_sub = img(jj:(jj+h-1),ii:(ii+wd-1),:);
                tmp_tex = Screen('MakeTexture',w, img_sub);
                sprites(i,j) = {tmp_tex};
            end
        end
    tex_struct.(fname) = sprites;

    elseif ismember(fname, well_sprites)
        %2 rows, 7 columns
        wellsprites = cell(7,2);
        [x,y,z] =size(img); 
        h= floor(x/2);
        wd = floor(y/7);
   
        blackPixels = img(:,:,1) == 0 & img(:,:,2) == 0 & img(:,:,3) == 0;
        alpha(blackPixels) = 0;

        img(:,:,4)= alpha; 

        for i=1:7 % i is width
            for j=1:2 % j is height
                 ii=(i-1)*wd+1; 
                 jj=(j-1)*h+1;
                img_sub = img(jj:(jj+h-1),ii:(ii+wd-1),:);
                tmp_tex = Screen('MakeTexture',w, img_sub);
                wellsprites(i,j) = {tmp_tex};
            end
        end
    tex_struct.(fname) = wellsprites;
    else
        tex_struct.(fname) = Screen('MakeTexture',w, img);
    end
end

end
