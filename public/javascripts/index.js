var enter = false;
var cnt = true;
var iFrequency = 1000;
var myInterval = 0;

$(document).keypress(function(e) {
    if (e.keyCode == 13 && enter) {
        e.preventDefault();
        next();
    }
});

$(document).ready(function() {
	startLoop();
	$('#myModal').modal({
	    backdrop: 'static',
	    keyboard: false
	});
	$('#myModal').modal('hide');
	checkColor();
	$(".list-group-item").click(function() {
		if (this.id == "counter") {
			return;
		}
		if (!cnt) {
			return;
		}
		$.ajax({
			  url: "/unifaces/application/answer?questionId=" + this.parentElement.id + "&answer=" + this.id
			}).done(function(data, textStatus, xhr) {
				cnt = false;
			    try {
			    	var obj = jQuery.parseJSON(data);
			    } catch(e) {
					window.location.href = "/unifaces";
			    }
				if (obj.correct == "true") {
					$("#modal-title").html("<b>Right +20</b>");
					$("#points").html(parseInt($("#points").html()) + 20);
					$("#modal-title").removeClass("red").addClass("green");
					checkColor();
				} else {
					$("#modal-title").html("<b>Wrong -10</b>");
					$("#points").html(parseInt($("#points").html()) - 10);
					$("#modal-title").removeClass("green").addClass("red");
					checkColor();
				}
				$('#modal-body').html(obj.info);
				$('#myModal').modal('show');
				enter = true;
			});
	});
});

function next() {
	$('#myModal').modal('hide');
	enter = false;
	$.ajax({
		  url: "/unifaces/application/next"
		}).done(function(data, textStatus, xhr) {
			try {
				var obj = jQuery.parseJSON(data);
			} catch (e) {
				window.location.href = "/unifaces";
			}
			cnt = true;
			$("#counter").html("<center id=\"counter-number\">10</center>");
			$("#ava").attr("src", "/unifaces/application/getPhoto" + "?questionId=" + obj.id);
			$("ul").toArray()[0].id = obj.id;
			$("li").toArray()[0].id = obj.id1;
			$("li").toArray()[1].id = obj.id2;
			$("li").toArray()[2].id = obj.id3;
			$("li").toArray()[3].id = obj.id4;
			$("li").toArray()[0].innerHTML = obj.name1;
			$("li").toArray()[1].innerHTML = obj.name2;
			$("li").toArray()[2].innerHTML = obj.name3;
			$("li").toArray()[3].innerHTML = obj.name4;
		});
		$("#points").html(parseInt($("#points").html()));
}

function checkColor() {
	var a = $("#points").html();
	if (a < 0) {
		$("#points").removeClass("green").addClass("red")
	} else if (a > 0) {
		$("#points").removeClass("red").addClass("green");
	} else {
		$("#points").removeClass("red").removeClass("green");
	}
}

function startLoop() {
    if (myInterval > 0) clearInterval(myInterval);
    myInterval = setInterval("doSomething()", iFrequency);
}

function doSomething() {
	if (cnt) {
		var nmb = parseInt($("#counter-number").html());
		if (nmb > 1) {
			$("#counter-number").html(nmb - 1);
		} else {
			$("#counter-number").html(0);
			cnt = false;
			$("#modal-title").html("<b>Time is up! -10</b>");
			$("#points").html(parseInt($("#points").html()) - 10);
			$("#modal-title").removeClass("green").addClass("red");
			$('#modal-body').html("<center>:(</center>");
			$('#myModal').modal('show');
			enter = true;
			checkColor();
		}
	}
}