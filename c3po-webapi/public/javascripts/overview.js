$(document).ready(
		function() {
			var button = $('<a  href="#" class="green_button">Add Diagram</a>').appendTo($('#more'));
			var buttonBubbleCharts = $('<a  href="#" class="green_button">Add Bubble Chart</a>').appendTo($('#bubble'));
			button.click(function() {
				$.ajax({
					headers : {
						Accept : "application/json; charset=utf-8",
					},
					type : 'GET',
					url : '/c3po/properties',
					timeout : 5000,
					async : false,
					success : function(oData) {
						showPopup(oData);

					}
				});
			});
			
			buttonBubbleCharts.click(function() {
				$.ajax({
					headers : {
						Accept : "application/json; charset=utf-8",
					},
					type : 'GET',
					url : '/c3po/properties',
					timeout : 5000,
					async : false,
					success : function(oData) {
						showBubbleChartPopup(oData);

					}
				});
			});

		});


function showPopup(properties) {
	$("#overlay").addClass('activeoverlay');

	var popup = $('#filterpopup');
	popup.children('.popupreason').text('Please select a property');
	var config = popup.children('.popupconfig');

	var sel = $('<select>').appendTo($(config));
	$(sel).append($('<option>').text("").attr('value', ''));
	$.each(properties, function(i, value) {
		$(sel).append($('<option>').text(value).attr('value', value));
	});

	popup.css({
		'display' : 'block',
		'z-index' : 11
	});

	$('.popupconfig select').change(function() {
		$.ajax({
			type : 'GET',
			url : '/c3po/property?name=' + $(this).val(),
			timeout : 5000,
			success : function(oData) {
				showOptions(oData.type, false);
			}
		});
	});
};


function showBubbleChartPopup(properties) {
	$("#overlay").addClass('activeoverlay');

	var popup = $('#filterpopup');
	popup.children('.popupreason').text('Please select a property pair.');
	var config = popup.children('.popupconfig');

	// property 1 selection
	var row1 = $('<div>x:&nbsp;</div>').appendTo($(config));
	
	var sel = $('<select id="prop1">').appendTo($(row1));
	$(sel).append($('<option>').text("").attr('value', ''));
	$.each(properties, function(i, value) {
		$(sel).append($('<option>').text(value).attr('value', value));
	});

	$(config).append($('<br />'));

	// property 2 selection
	var row2 = $('<div>y:&nbsp;</div>').appendTo($(config));

	var sel2 = $('<select id="prop2">').appendTo($(row2));
	$(sel2).append($('<option>').text("").attr('value', ''));
	$.each(properties, function(i, value) {
		$(sel2).append($('<option>').text(value).attr('value', value));
	});
	
	popup.css({
		'display' : 'block',
		'z-index' : 11
	});


	// on change of one property select
	$('.popupconfig select').change(function() {
		var select = $(this);
		var value = $(select).val();
		// build ids 
		var thisId = $(select).attr("id");
		var algId = thisId + "_alg";
		var widthId = thisId + "_width";
		// reset selction 
		$(select).removeData();	// remove stored values
		$('#' + algId).remove();
		$('#' + widthId).remove();
		
		if (value) {
			// get property (name, type...)
			$.ajax({
				type : 'GET',
				url : '/c3po/property?name=' + value,
				timeout : 5000,
				success : function(oData) {
					// show width method and selection if this is a numeric property
					$(select).data('type', oData.type);
					if (oData.type == "INTEGER" || oData.type == "FLOAT") {
    					$(select).parent().append($(
    						'<select id=\"' + algId + '\"><option/><option value="fixed">fixed</option><option value="sturge">Sturge\'s</option><option value="sqrt">Square-root choice</option></select>'));
					}
					$('#' + algId).change(function() {
						$(select).data('alg', $(this).val());
						if ($(this).val() == "fixed") {
							$('#' + algId).parent().append($('<input id=\"' + widthId + '\" type="text" placeholder="bin width" />'));
						}
					});
				}
			});
		}
	});
	
	$(config).append($('<br />'));
	$(config).append($('<br />'));

	// apply button
	var row3 = $('<div align="right" style="padding-right: 3em;" />').appendTo($(config));
	var apply = $('<a class="green_button" href="#" >apply</a>').appendTo($(row3));
	apply.click(function() {
		// input validation
		if ($('#prop1').val() == $('#prop2').val()) {
			alert("please select different properties");
			return;
		}
		
		// build url and check input values
		var url = "/c3po/overview/bubblegraph?";
		for (var i = 1; i <= 2; i++) {
    		var type = $('#prop' + i).data('type');
    		if (!type) {
    			alert("no value for property " + i + " selected");
    			return;
    		}
    		url += "property" + i + "=" + $('#prop' + i).val();
    		if (type == "INTEGER" || type == "FLOAT") {
    			var alg = $('#prop' + i).data('alg');
    			if (!alg) {
    				alert("no bin width method selected for numeric property " + i);
    				return;
    			}
    			url += "&alg" + i + "=" + alg;
    			if (alg == "fixed") {
    				var width = $('#prop' + i + '_width').val();
    				if (!$.isNumeric(width)) {
    					alert("given bin width for numeric property " + i + " is not a valid number");
    					return;
    				}
    				url += "&width" + i + "=" + width;
    			}
    		}
    		url += "&";
		}
		
		// we have all needed values, do the graph fetching
		startSpinner();
		$.ajax({
			type : 'GET',
			url : url,
			timeout : 5000,
			success : function(oData) {
    			stopSpinner();
				hidePopupDialog();
    			var data = {};
    			data[oData.title] = {
    			                     type: oData.type,
    			                     data: oData.graphData,
    			                     options: oData.graphOptions
    			                     };
    			
    			drawGraphs(data, oData.options);
    		} // end success function
    	}); // end ajax call
	}); // end apply.click 

};

function showOptions(type, bubble) {
	if (type == "STRING" || type == "BOOL" || type == "DATE") {
		var property = $('.popupconfig select').val();
		hidePopupDialog();
		startSpinner();
		$.ajax({
			type : 'GET',
			url : '/c3po/overview/graph?property=' + property,
			timeout : 5000,
			success : function(oData) {
				stopSpinner();
				var hist = [];
				$.each(oData.keys, function(i, k) {
					hist.push([ oData.keys[i], parseInt(oData.values[i]) ]);
				});
				var id = oData.property;
				var data = {};
				data[id] = {
				         type: 'histogram',
				         data: hist,
				         options: null
				         };
				
				
				drawGraphs(data);
				//scroll to bottom of page.

			}
		});

	} else {
		if(!bubble) {
			showIntegerPropertyDialog('applyIntegerHistogramSelection()');
		}
		else {
			var alg = "sqrt";
			var width = -1;
		}
		
	}
}

function applyIntegerHistogramSelection() {
	var selects = $('.popupconfig').children('select');
	var property = $('.popupconfig').children('select:first').val();
	var alg = $('.popupconfig').children('select:last').val();
	var width = -1;
	if (alg == "fixed") {
		width = $('.popupconfig input:first').val();
	}

	hidePopupDialog();
	startSpinner();
	$.ajax({
		type : 'GET',
		url : '/c3po/overview/graph?property=' + property + "&alg=" + alg
				+ "&width=" + width,
		timeout : 5000,
		success : function(oData) {
			var hist = [];
			$.each(oData.keys, function(i, k) {
				hist.push([ oData.keys[i], parseInt(oData.values[i]) ]);
			});
			var id = oData.property;
			var data = {};
			data[id] = {
            	         type: 'histogram',
            	         data: hist,
            	         options: null
	         			};
			$('#' + id).remove(); // remove the old graph if exist
			drawGraphs(data, oData.options);
			stopSpinner();
			//scroll to bottom of page.
		}
	});
};

function getBarChart(ttl) {
	var options = {
		title : ttl,
		seriesDefaults : {
			renderer : $.jqplot.BarRenderer,
			// Show point labels to the right ('e'ast) of each bar.
			// edgeTolerance of -15 allows labels flow outside the grid
			// up to 15 pixels. If they flow out more than that, they
			// will be hidden.
			pointLabels : {
				show : true,
				location : 'n',
				edgeTolerance : -15
			},
			// Rotate the bar shadow as if bar is lit from top right.
			shadowAngle : 70,
			// Here's where we tell the chart it is oriented horizontally.
			rendererOptions : {
				barDirection : 'vertical',
				barWidth : '12'
			},
			color : '#639B00'
		},
		axesDefaults : {
			tickRenderer : $.jqplot.CanvasAxisTickRenderer,
			tickOptions : {
				angle : -30,
				fontSize : '8pt'
			}
		},
		axes : {
			// Use a category axis on the x axis and use our custom ticks.
			xaxis : {
				renderer : $.jqplot.CategoryAxisRenderer,
				tickOptions : {
					formatter : function(format, val) {
						if (val.length > 30) {
							val = val.substring(0, 25) + '...';
						}

						// val = (val.replace(/\.0/g, ""));
						return val;
					}
				}
			},
			// Pad the y axis just a little so bars can get close to, but
			// not touch, the grid boundaries. 1.2 is the default padding.
			yaxis : {
				pad : 1.05,
				tickOptions : {
					formatString : '%d',
				}
			}
		},
		highlighter : {
			show : true,
			tooltipLocation : 'n',
			showTooltip : true,
			useAxesFormatters : true,
			sizeAdjust : 0.5,
			tooltipAxes : 'y',
			bringSeriesToFront : true,
			tooltipOffset : 30,
		},
		cursor : {
			style : 'pointer', // A CSS spec for the cursor type to change the
								// cursor to when over plot.
			show : true,
			showTooltip : false, // show a tooltip showing cursor position.
			useAxesFormatters : true, // wether to use the same formatter and
										// formatStrings
		// as used by the axes, or to use the formatString
		// specified on the cursor with sprintf.
		}

	};

	return options;
};

function getPieChart(ttl) {
	var options = {
		title : ttl,
		seriesDefaults : {
			renderer : $.jqplot.PieRenderer,
			rendererOptions : {
				showDataLabels : true
			}
		},
		legend : {
			show : true,
			location : 'e'
		}
	};

	return options;
};

function getBubbleChart(ttl) {
	var options = {
			title : ttl,
			seriesDefaults : {
				   // set transparency, color, label of bubbles
		           renderer : $.jqplot.BubbleRenderer,
		           rendererOptions : {
		               bubbleAlpha : 0.6,
		               highlightAlpha : 0.8,
		               varyBubbleColors : false,
		               color : '#639B00',
		               bubbleGradients: true,
		               showLabels: false
		           },
		           // shadow for bubbles
		           shadow : true,
		           shadowAlpha : 0.05,
		           
			},
			// axis labeling
			axesDefaults : {
				tickRenderer : $.jqplot.CanvasAxisTickRenderer,
				tickOptions : {
					angle : -30,
					fontSize : '8pt',
        			formatter : function(format, val) {
        				if (val.length > 30) {
        					val = val.substring(0, 25) + '...';
        				}
        				return val;
        			}
				}
			},
			// how to render the labels for axes
			axes : {
				xaxis : {
					renderer: $.jqplot.CategoryAxisRenderer,
				},
				yaxis : {
					renderer: $.jqplot.CategoryAxisRenderer,
				}
			},	
			// how the bubble behaves when highlighted by mouse over event
			highlighter : {
				show : true,
				tooltipLocation : 'n',
				showTooltip : true,
				useAxesFormatters : true,
				sizeAdjust : 0.5,
				tooltipAxes : 'xy',
				bringSeriesToFront : true,
				tooltipOffset : 50,
			},
			// cursor style over plot area
			cursor : {
				style : 'pointer', 
				show : true,
				showTooltip : false,
				useAxesFormatters : true,
			}
		};
		return options;
}

function prettifyTitle(title) {
	title = title.replace(/_/g, " ");
	return title.replace(/\w\S*/g, function(txt) {
		return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();
	});
};

function drawGraphs(data, options) {
	var idx = 0;
	var graphsdiv = $('#graphs');
	$.each(data, function(i, d) {
		var container;
		var clazz;
		if (idx % 2 == 0) {
			container = $('<div class="span-24">').appendTo(graphsdiv);
			clazz = "dia_left";
		} else if (idx % 2 == 1) {
			container = graphsdiv.children('.span-24:last');
			clazz = "dia_right";
		}

		if (d.data.length > 30) {
			container = $('<div class="span-24">').appendTo(graphsdiv);
			clazz = "dia_full";
			idx++; // if full length skip to next row left
		}

   	    $('#' + i).remove(); // remove old graph, it it exists
		container.append('<div id="' + i + '" class="' + clazz + '">');
		$('#' + i).bind(
				'jqplotDataClick',
				function(ev, seriesIndex, pointIndex, data) {
					startSpinner();
					var url;
					
					// if it is a bubble chart the url has to contain both properties and both values
					if (d.type == "bubblechart") {
						var properties = i.split("_");
						var values = data[3].split(" - ");
						url = '/c3po/bubblefilter?' + 
							'property0=' + options['property1'] +
							'&property1=' + options['property2'] +
							'&index=' + pointIndex +
							'&type=graph';
						if (options) {
							url += '&alg1=' + options['alg1'];
							url += '&alg2=' + options['alg2'];
							url += '&width1=' + options['width1'];
							url += '&width2=' + options['width2'];
						}
					}
					else {
						url = '/c3po/filter?filter=' + i + '&value=' + pointIndex + '&type=graph';
					}
					

					if (options) {
						var type = options['type'];
						var alg = options['alg'];
						var width = options['width'];

						if (type == 'INTEGER') {
							url += '&alg=' + alg;

							if (width) {
								url += '&width=' + width;
							}
						}
					}
					$.post(url, function(data) {
						window.location = '/c3po/overview';
					});
				});
		if (d.type == "histogram") {		
		  $.jqplot(i, [ d.data ], getBarChart(prettifyTitle(i)));
		} else if (d.type == "bubblechart") {
		  // remove old tooltip if exists
		  $('#tooltip' + i).remove();
		  // build title
		  var title = prettifyTitle(i); // default title
		  if (options) {
			  title = prettifyTitle(options['property1']) + ' (x) - ' +
			          prettifyTitle(options['property2']) + ' (y)';
		  }
		  // draw a bubble chart
		  var plot = $.jqplot(i, [ d.data ], getBubbleChart(title));
		  // draw a tool tip to display the data
		  // (stolen from http://www.jqplot.com/deploy/dist/examples/bubbleChart.html)
		  $('#' + i).parent().append('<div style="position:absolute;z-index:99;display:none;background-color:#fff;padding:0.5em" id="tooltip' + i + '"></div>');
		  $('#' + i).bind('jqplotDataHighlight',
		        function (ev, seriesIndex, pointIndex, data, radius) {   
		            var chart_left = $('#' + i).offset().left,
		                chart_top = $('#' + i).offset().top,
		                x = plot.axes.xaxis.u2p(data[0]),  // convert x axis unita to pixels on grid
		                y = plot.axes.yaxis.u2p(data[1]);  // convert y axis units to pixels on grid
		            $('#tooltip' + i).css({left:chart_left+x+radius+5, top:chart_top+y});
		            $('#tooltip' + i).html('<span style="font-size:14px;font-weight:bold;color:#639B00;">' +
		            data[3] + '</span><br />' + 'count: ' + data[2]);
		            $('#tooltip' + i).show();
		        });
		     
		    // Bind a function to the unhighlight event to clean up after highlighting.
		    $('#' + i).bind('jqplotDataUnhighlight',
		        function (ev, seriesIndex, pointIndex, data) {
		            $('#tooltip' + i).empty();
		            $('#tooltip' + i).hide();
		        });		  
		}

		if (idx == 0) {
			idx++; // if first row skip the right and go to next row...
		}
		idx++;
	})
};


