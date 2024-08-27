function sprites = load_astro()

[img, ~, alpha] = imread('out/imgs/astronaut.png');

% 4 rows 3 cols
sprites = cell(4,4);
[x,y,z] =size(img); % z was missing, added 20240723 SDM
h= 75;
wd = floor(y/4);
for i=1:4
    for j=1:4
        ii=(i-1)*wd+1; 
        jj=(j-1)*h+1;
        img_sub = img(jj:(jj+h-1),ii:(ii+wd-1),:); %(height, width, z)
        % tmp_tex = Screen('MakeTexture',w, img_sub);
        sprites(i,j) = {img_sub};
    end
end
