(function($){
	$.urlParam = function(name) {
		return (RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.search)||[,null])[1];
	};
	
	$.fn.table = function(options) {
		var settings = $.extend({}, options);
		
		return this.each(function() {
			var table = $(this);
			table.empty();
			var thead =  $('<thead/>').appendTo(table);
			var tr =  $('<tr/>').appendTo(thead);
			$.each(settings.headers, function(index, header) {
				if (typeof header == 'string') {
					header = {
						text: header
					};
				}
				header = $.extend({
					colspan:1
				}, header);
				
				var td = {};
				td.text = header.text;
				if (header.colspan > 1) {
					td.colspan = header.colspan;
				}
				$('<td/>', td).appendTo(tr);
			});
			var tbody =  $('<tbody/>').appendTo(table);
		});
	};
	
	$.fn.dataTable = function(data, options) {
		var settings = $.extend({
			headers: []
		}, options);
		
		$.each(settings.headers, function(index, header) {
			if (!header.renderer) {
				header.renderer = function(tr, element, property) {
					$('<td/>', {text: element[property]}).appendTo(tr);
				};
			}
		});
		
		function colCount(table) {
			var count = 0;
			table.find("thead > tr > td").each(function() {
				if ($(this).attr('colspan')) {
					count += +$(this).attr('colspan');
				} else {
					count++;
				}
			});
			return count;
		}
		
		var table = this.table(options);
		table.each(function() {
			$(this).children('tbody').loadData(data, {
				onLoading: function(container) {
					container.empty();
					var tr = $('<tr/>').appendTo(container);
					$('<td/>', {colspan:colCount(table), text: 'carregando...'}).appendTo(tr);
				},
				onData: function(container) {
					container.empty();
				},
				onElement: function(container, index, element) {
					var tr = $('<tr/>').appendTo(container);
					$.each(settings.headers, function(index, header) {
						header.renderer.apply(this, [tr, element, header.property]);
					});
				},
				onFail: function(container) {
					container.empty();
					var tr = $('<tr/>').appendTo(container);
					$('<td/>', {colspan:colCount(table), text: 'falha'}).appendTo(tr);
				}
			});
		});
	};
	
	//Carrega dados remotos 
	$.fn.loadData = function(data, options) {
		var settings = $.extend({
			onLoading: function() {},
			onData: function() {},
			onElement: function() {},
			onFail: function() {}
		}, options);
		
		this.each(function() {
			var container = $(this);
			settings.onLoading.apply(container, [container]);
			
			function done(data) {
				settings.onData.apply(container, [container, data]);
				$.each(data, function(key, val) {
					settings.onElement.apply(container, [container, key, val]);
				});
			}
			
			function fail(jqXHR, textStatus) {
				settings.onFail.apply(container, [container]);
			}
			
			if (typeof data == 'string') {
				$.getJSON(data, done).fail(fail);
			} else if (typeof data == 'object') {
				done(data);
			}
		});
	};
})(jQuery);