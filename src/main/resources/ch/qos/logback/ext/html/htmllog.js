<script type="text/javascript">
/* Stack Trace Toggling */

function getElementById(id) {
  var element;

  if (document.getElementById) { // standard
    return document.getElementById(id);
  } else if (document.all) { // old IE versions
    return document.all[id];
  } else if (document.layers) { // nn4
    return document.layers[id];
  }
  alert("Sorry, but your web browser is not supported by Concordion.");
}

function isVisible(element) {
  return element.style.display;
}

function makeVisible(element) {
  element.style.display = "block";
}

function makeInvisible(element) {
  element.style.display = "";
}

function toggleStackTrace(stackTraceNumber) {
  var stackTrace = getElementById("stackTrace" + stackTraceNumber);
  var stackTraceButton = getElementById("stackTraceButton" + stackTraceNumber);
  if (isVisible(stackTrace)) {
    makeInvisible(stackTrace);
    stackTraceButton.value = "View Stack";
  } else {
    makeVisible(stackTrace);
    stackTraceButton.value = "Hide Stack";
  }
}

/* Image popup */
function showScreenPopup(src) {
	var img = document.getElementById('ScreenshotPopup');
	img.src = src.src
	
	var scrollTop = Math.max(document.body.scrollTop, document.documentElement.scrollTop);
	var scrollLeft = Math.max(document.body.scrollLeft, document.documentElement.scrollLeft);
	var viewportWidth = window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;
	var viewportHeight = window.innerHeight || document.documentElement.clientHeight || document.body.clientHeight;
	var scrollbarWidth = viewportWidth - document.body.offsetWidth;
	
	var srcRect = src.getBoundingClientRect();
	var imgRect = img.getBoundingClientRect();
	var naturalWidth = (img.naturalWidth || 0);
	var naturalHeight = (img.naturalHeight || 0);

	var above = srcRect.top > (viewportHeight - srcRect.top - srcRect.height);
	var posTop = 0;
	var posLeft = 0;
	
	if (above) {
		var height = Math.min(naturalHeight, srcRect.top - 10)
		posTop = scrollTop + srcRect.top - height;
		
		img.style.height = height + "px";
		img.style.width = 'auto';
		
		if (img.width > Math.min(naturalWidth, viewportWidth)) {
			img.style.height = 'auto';
			img.style.width = Math.min(naturalWidth, viewportWidth) + "px";
			
			posTop = scrollTop + srcRect.top - img.height;
		}
	} else {
		var height = Math.min(naturalHeight, viewportHeight - srcRect.top - srcRect.height - 10)
		posTop = scrollTop + srcRect.top + srcRect.height;
		
		img.style.height = height + "px";
		img.style.width = 'auto';
		
		if (img.width > Math.min(naturalWidth, viewportWidth)) {
			img.style.height = 'auto';
			img.style.width = Math.min(naturalWidth, viewportWidth) + "px";
		}
	}

	var posLeft = srcRect.left + scrollLeft + 1;
	   
	if (posLeft + img.width > scrollLeft + viewportWidth - scrollbarWidth) {
		posLeft = scrollLeft + viewportWidth - img.width - scrollbarWidth;
	}
	if (posLeft < scrollLeft) {
		posLeft = scrollLeft;
	}

	img.style.left = posLeft + "px";
	img.style.top = posTop + "px";
	img.style.visibility = 'visible';	
}

function hideScreenPopup() {
	document.getElementById('ScreenshotPopup').style.visibility = 'hidden';
}
</script>