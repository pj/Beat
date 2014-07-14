var dashIdx = 0;
var dashOffset = 0;
var dashValues = [10, 10];
var dashX = 0;
var dashY = 0;

function setDash(dashes)
{
	dashValues = dashes;
	dashIdx = 0;
	dashOffset = dashValues[0];
}

function dashMoveTo(ctx, x, y)
{
	dashX = x;
	dashY = y;
}

function dashLineTo(ctx, x1, y1)
{
	var length = Math.sqrt( (x1-dashX)*(x1-dashX) + (y1-dashY)*(y1-dashY) );
	var dx = (x1-dashX) / length;
	var dy = (y1-dashY) / length;

	var dist = 0;
	while (dist < length)
	{
		var dashLength = Math.min(dashValues[dashIdx], length-dist);
		dist += dashLength;

		if (dashIdx % 2 == 0)
			ctx.moveTo(dashX, dashY);

		dashX += dashLength * dx;
		dashY += dashLength * dy;

		if (dashIdx % 2 == 0)
			ctx.lineTo(dashX, dashY);

		dashOffset += dashLength;
		if (dashOffset > dashValues[dashIdx])
		{
			dashOffset -= dashValues[dashIdx];
			dashIdx = (dashIdx+1) % dashValues.length;
		}
	}
}