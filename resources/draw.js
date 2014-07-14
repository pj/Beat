function logSize (argument) {
	console.log("width: " + window.innerWidth + " height: " + 
				window.innerHeight + " scroll x: " + window.pageXOffset + 
				" scroll y: " + window.pageYOffset);
}

function handleScroll (event) {
	draw();
}

function handleResize (event) {
	draw();
}

// draw thread name

var nameDrawGap = 300;

function drawThreadName(ctx, color, line, name, y, height){
	if(line[0][0] == line[1][0]){
		start = line[0][1] - ((y + line[0][1]) % nameDrawGap);
		
		while(true){
			if(start > line[1][1]){
				break;
			}
			
			if(start < line[0][1]){
				start += nameDrawGap;
				continue;
			}
			
			ctx.save();
			ctx.font = "14px bold Monaco";
			ctx.fillStyle = color;

			ctx.translate(line[0][0]+5, start);

			ctx.rotate(Math.PI/2);
			ctx.fillText(name, 0, 0);

			ctx.restore();
			
			start += nameDrawGap;
		}
	}
}

function draw () {
	canvas = $('#threads')[0];  
	ctx = canvas.getContext('2d');
	
	width = window.innerWidth;
	height = window.innerHeight;
	x = window.pageXOffset;
	y = window.pageYOffset;

	canvas.width = width;
	canvas.height = height;

	ctx.clearRect(0,0, width, height);
	
	// set up for drawing
	threadCount = drawSetup(x, y, width, height);
	
	// draw each thread
	for(n = 0; n < threadCount; n++){
		points = getDrawPoints(n);
		name = getThreadName(n);
				
		if(points == null){
			continue;
		}

		color = getDrawColor(n);
		
		for(x = 0; x < points.length; x++){
			types = getDrawType(x);
			type = types[0];
			end_type = types[1];
			
			if(type == "threadDeathException" && end_type == "exit"){
				break;
			}
			
			line = points[x];
			
			line_type = "normal";
			
			if(type == "join" || type == "wait" || type == "start" || type == "sleep" || type == "switch" || type == "blockAcquire"){
				line_type = "dashed";
			}
			
			ctx.strokeStyle = color;
			ctx.lineWidth = 3;
			ctx.lineCap = "round";
			
			ctx.beginPath();
			
			if(type == "start"){
				setDash([8,8]);
			
				dashMoveTo(ctx, line[0][0], line[0][1]);
			
				if(line[1][0] < line[0][0]){
					dashLineTo(ctx, line[1][0]+(icon_width/2), line[1][1]);
				}else{
					dashLineTo(ctx, line[1][0]-(icon_width/2), line[1][1]);
				}
				
				dashMoveTo(ctx, line[1][0], line[1][1]+(icon_height/2));
				
				dashLineTo(ctx, line[2][0], line[2][1]);
				
				ctx.stroke();
				
				drawIcon(ctx, line[1][0], line[1][1]-(icon_height/2)-1, type, color);
			}else if(line_type == "dashed"){
				setDash([8,8]);
			
				if(type != "blockAcquire"){
					dashMoveTo(ctx, line[0][0], line[0][1] + icon_height+2);
				}else{
					dashMoveTo(ctx, line[0][0], line[0][1]);
				}
			
				drawIcon(ctx, line[0][0], line[0][1], type, color);
				
				ctx.lineWidth = 3;
				ctx.lineCap = "round";
			
				dashLineTo(ctx, line[1][0], line[1][1]);
				
				ctx.stroke();
				
				drawThreadName(ctx, color, line, name, y, height);
			}else{
				if(type == "notify" || type == "thrown" || type == "exception"){
					drawIcon(ctx, line[0][0], line[0][1], type, color);
					ctx.moveTo(line[0][0], line[0][1] + icon_height+2);
				}else{
					ctx.moveTo(line[0][0], line[0][1]);
				}
				
				ctx.lineTo(line[1][0], line[1][1]);

				ctx.stroke();
				
				drawThreadName(ctx, color, line, name, y, height);
				
				if(end_type == "exit" || end_type == "threadDeathException"){
					drawIcon(ctx, line[1][0], line[1][1], end_type, color);
				}
			}
			
			
		}
	}
	
	// drawStart(ctx, icon_width+10, 30, "red");
	// drawWait(ctx, (icon_width *2)+10*2, 30, "red");
	// drawSleep(ctx, (icon_width *3)+10*3, 30, "red");
	// drawJoin(ctx, (icon_width *4)+10*4, 30, "red");
	// drawNotify(ctx, (icon_width *5)+10*5, 30, "red");
	// drawExit(ctx, (icon_width *6)+10*6, 30, "red");
}



function setResize(){
	setColumns();
	
	draw();
}

function setColumns(){
	$(".object-column").each(
		function(i){
			id = $(this).attr("id");
			width = $(this).width();
			left = $(this).position().left;
			
			$("#" + id + " .no_indent").css("width",(width-35) + "px").css("left", (20) + "px");
			
			for(x = 0; x <= cssIndent; x++){
				for(n = 0; n <= x; n++){
					log(" .indent_" + n + "_" + x);
					log(Math.round(20 + (width*(n/x))));
					$("#" + id + " .indent_" + n + "_" + x).css("width",Math.round((width/x)-35) + "px").css("left", Math.round(20 + (width*(n/x))) + "px");
				}
			}
		

			

			// $("#" + id + " .object-header").css("width",(width) + "px");
			setColumnWidth(id, width, $(this).position().left, i);
		}	
	);
}

// function canvasMove(event){
// 	element = $("#visualization").elementFromPoint(event.clientX, event.clientY);
// 	log(element.id);
// }

function eventMove(event){
	$("#event-details").empty();
	$("#event-details").append($(".event-detail", event.target).clone());
	$("#event-details .event-detail").attr("class", "event-detail-display");
	$("#event-details .event-detail-display").css("display", "block");
}

$(document).ready(
	function(event){
		$(".object-column").each(//.slice(0,-1).each(
			function(i){
				$(this).resizable({handles: 'e', distance: 15, resize: setResize});
			}
		);
		
		// $("#threads").mousemove(canvasMove);
		
		$(".event").mousemove(eventMove);
				
		$(window).scroll(handleScroll);
		$(window).resize(handleResize);
		
		$("#visualization").sortable({
			items: ".object-column",
			axis: 'x',
			handle: ".object-header",
			
			stop: function (event, ui){
				setColumns();
				
				draw();
			}
		});
	}
);
