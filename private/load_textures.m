function tex_struct = load_textures(w, varargin)
% read images into struct of named textures tex_struct.ocean_bottom == 32
  fprintf('# loading textures\n');
  imgs = [dir('out/imgs/*.png')];

  avatar_sprites = {'astronaut', 'shark','lizard_blue', 'alient_green'};
  well_sprites = {'chest_sprites', 'wellcoin_sprites'};
  for f = imgs'
     [~, fname, ~]  = fileparts(f.name); fname(fname=='-')='_';
     [img, ~, alpha] = imread(fullfile(f.folder,f.name));
     img(:,:,4) = alpha;
     % TODO: scale images to screen size

     % sprites within one file. have animations frames in a grid
     if ismember(fname, avatar_sprites)
        % 4 rows 3 cols
        sprites = cell(4,4);
        [x,y,_] =size(img);
        h= floor(y/4);
        wd = floor(x/4); 
        for i=1:4; for j=1:4;
          ii=(i-1)*h+1; jj=(j-1)*h+1;
          img_sub = img(jj:(jj+wd-1),ii:(ii+h-1),:);
          tmp_tex = Screen('MakeTexture',w, img_sub);
          sprites(i,j) = tmp_tex;
        end;end
        tex_struct.(fname) = sprites;
     else
        tex_struct.(fname) = Screen('MakeTexture',w, img);
     end
  end
  
end
