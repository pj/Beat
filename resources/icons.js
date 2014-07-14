var icon_width = 30;
var icon_height = 20;

function drawIcon(ctx, x, y, type, color){
	x2 = x - (icon_width/2);
	y2 = y+1;
	
	if(type == "wait"){
		drawWait(ctx, x2, y2, color);
	}else if(type == "start"){
		drawStart(ctx, x2, y2, color);
	}else if(type == "join"){
		drawJoin(ctx, x2, y2, color);
	}else if(type == "sleep"){
		drawSleep(ctx, x2, y2, color);
	}else if(type == "exit"){
		drawExit(ctx, x2, y2, color);
	}else if(type == "notify"){
		drawNotify(ctx, x2, y2, color);
	}else if(type == "thrown"){
		drawThrown(ctx, x2, y2, color);
	}else if(type == "exception"){
		drawException(ctx, x2, y2, color);
	}else if(type == "threadDeathException"){
		drawDeath(ctx, x2, y2, color);
	}
}

function drawDeath(ctx, x, y, color){
	drawBox(ctx, x, y, color);
	
	// draw E
	
	ctx.strokeStyle = color;
	ctx.lineWidth = 3;
	ctx.lineCap = "square";
	
	center_x = (icon_width/2)+x;
	
	ctx.beginPath();
	
	ctx.moveTo(center_x-4, y+5);
	ctx.lineTo(center_x-4, y+icon_height-5);
	
	ctx.moveTo(center_x-4, y+5);
	ctx.lineTo(center_x+4, y+5);
	
	ctx.moveTo(center_x-4, y+10);
	ctx.lineTo(center_x+2, y+10);
	
	ctx.moveTo(center_x-4, y+icon_height-5);
	ctx.lineTo(center_x+4, y+icon_height-5);
	
	ctx.stroke();
	
	ctx.lineCap = "butt";
}

function drawThrown(ctx, x, y, color){
	drawBox(ctx, x, y, color);
	
	// draw T
	
	center_x = (icon_width/2)+x;
	
	ctx.strokeStyle = color;
	ctx.lineWidth = 3;
	ctx.lineCap = "square";
	
	ctx.beginPath();
	
	ctx.moveTo(center_x, y+5);
	
	ctx.lineTo(center_x, y+icon_height-5)
	
	ctx.moveTo(center_x-6, y+5);
	
	ctx.lineTo(center_x+6, y+5);
	
	ctx.stroke();
	
	ctx.lineCap = "butt";
}

function drawException(ctx, x, y, color){
	drawBox(ctx, x, y, color);
	
	// draw C
	
	center_x = (icon_width/2)+x;
	
	ctx.beginPath();
	
	ctx.moveTo(center_x+4, y+10);
	
	ctx.bezierCurveTo(x,y,x,y+icon_height,center+4,y+icon_height-10);  
	
	ctx.stroke();
}

function drawStart(ctx, x, y, color){
	drawBox(ctx, x, y, color);
	
	center_y = (icon_height/2)+y;
	
	drawArrow(ctx, x+4, center_y, x+icon_width-5, center_y, 3, color);
}

function drawArrow(ctx, start_x, start_y, end_x, end_y, arrow_pos, color){
	ctx.strokeStyle = color;
	ctx.lineWidth = 2;
	
	ctx.beginPath();
	
	ctx.moveTo(start_x, start_y);
	
	ctx.lineTo(end_x, end_y);
	
	ctx.lineTo(end_x-arrow_pos, end_y-arrow_pos);
	
	ctx.moveTo(end_x, end_y);
	
	ctx.lineTo(end_x-arrow_pos, end_y+arrow_pos);
	
	ctx.stroke();
}


function drawWait(ctx, x, y, color){
	drawBox(ctx, x, y, color);
	
	center_x = (icon_width/2)+x;
	y_pos = y+icon_height-6;
	
	x_gap = 8;
	
	ctx.strokeStyle = color;
	ctx.lineWidth = 4;
	
	ctx.beginPath();
	
	ctx.moveTo(center_x-x_gap, y_pos);
	ctx.lineTo(center_x-x_gap, y_pos);
	
	ctx.moveTo(center_x, y_pos);
	ctx.lineTo(center_x, y_pos);
	
	ctx.moveTo(center_x+x_gap,y_pos);
	ctx.lineTo(center_x+x_gap,y_pos);
	
	ctx.stroke();
}

function drawSleep(ctx, x, y, color){
	drawBox(ctx, x, y, color);
	
	drawZ(ctx, x+4, y+4, 7, 12, color, 2);
	drawZ(ctx, x+14, y+6, 6, 9, color, 1.5);
	drawZ(ctx, x+22, y+8, 5, 6, color, 0.5);
}

function drawZ(ctx, x, y, width, height, color, lineW){
	ctx.strokeStyle = color;
	ctx.lineWidth = lineW;
	ctx.strokeColor = color;
	
	ctx.beginPath();
	
	ctx.moveTo(x, y);
	ctx.lineTo(x+width, y);
	
	ctx.lineTo(x, y+height);
	
	ctx.lineTo(x+width, y+height);
	
	ctx.stroke();
}

function drawJoin(ctx, x, y, color){
	drawBox(ctx, x, y, color);
	
	center_y = (icon_height/2)+y;
	center_x = (icon_width/2)+x;
	
	drawArrow(ctx, x+4, y+4, x+icon_width-8, center_y-2, 2, color);
	
	drawArrow(ctx, x+4, y+icon_height-4, x+icon_width-12, center_y+3, 2, color);
	
	ctx.strokeStyle = color;
	ctx.lineWidth = 4;
	
	ctx.beginPath();
	
	ctx.moveTo(x+icon_width-5, center_y);
	
	ctx.lineTo(x+icon_width-5, center_y);
	
	ctx.stroke();
}

function drawNotify(ctx, x, y, color){
	drawBox(ctx, x, y, color);
	
	center = (icon_width/2)+x;
	
	// draw circle
	ctx.strokeStyle = color;
	ctx.lineWidth = 1;
	ctx.fillStyle = color;
	
	ctx.beginPath();
	
	ctx.arc(center, y+20-3, 6, 0, Math.PI, true);
	
	ctx.fill();
	
	ctx.lineWidth = 3;
	
	// draw lines
	ctx.beginPath();
	
	ctx.moveTo(center,y+4);
	ctx.lineTo(center, y+8);
	
	ctx.moveTo(x+4, y+icon_height-5);
	ctx.lineTo(center-8, y+icon_height-5);
	
	ctx.moveTo(center+8, y+icon_height-5);
	ctx.lineTo(x+icon_width-4, y+icon_height-5);
	
	ctx.stroke();
}

function drawExit(ctx, x, y, color){
	drawBox(ctx, x, y, color);
	
	ctx.strokeStyle = color;
	ctx.lineWidth = 3;
	
	ctx.beginPath();
	
	ctx.moveTo(x,y);
	
	ctx.lineTo(x+icon_width, y+icon_height);
	
	ctx.moveTo(x+icon_width,y);
	
	ctx.lineTo(x, y+icon_height);
	
	ctx.stroke();
}

function drawBox(ctx, x, y, color){
	ctx.strokeStyle = color;
	ctx.lineWidth = 3;
	
	ctx.strokeRect(x,y,icon_width,icon_height);
}