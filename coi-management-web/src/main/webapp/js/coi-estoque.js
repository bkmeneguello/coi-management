"use strict";

COI.module("Estoque", function(Module, COI, Backbone, Marionette, $, _) {
	
	var EstoqueItem = Backbone.Model.extend({
		defaults: {
			data: null,
			produto: null,
			quantidade: null
		},
		toJSON: function(options) {
			var attributes = _.clone(this.attributes);
			attributes.data = toTimestamp(attributes.data);
			return attributes;
		}
	});

	var Estoque = Backbone.Paginator.requestPager.extend({
		url: 'rest/estoque',
		model: EstoqueItem,
		paginator_core: {
			type: 'GET',
			dataType: 'json',
			url: 'rest/estoque'
		},
		paginator_ui: {
			firstPage: 0,
			currentPage: 0
		},
		server_api: {
			'page' : function() {
				return this.currentPage;
			}
		}
	});
	
	var RowView = COI.ActionRowView.extend({
		template: '#estoque_row_template',
		onUpdate: function(e) {
			Backbone.history.navigate('movimento/' + this.model.get('id'), true);
		},
		onDelete: function(e) {
			_promptDelete(function() {
				e.model.destroy({
					wait: true,
					success: function(model, response, options) {
						_notifyDelete();
					},
					error: function(model, xhr, options) {
						_notifyDeleteFailure();
					}
				});
			});
		}
	});
	
	var EstoqueView = COI.GridView.extend({
		searchView: COI.SimplePageView,
		itemView: RowView,
		templateHelpers: {
			header: 'Estoque',
			columns: {
				data: 'Data',
				tipo: 'Tipo'
			}
		},
		initialize: function() {
			this.collection.fetch();
		},
		onCancel: function(e) {
			Backbone.history.navigate('', true);
		},
		onCreate: function(e) {
			Backbone.history.navigate('movimento', true);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('estoque', function() {
			COI.body.show(new EstoqueView({collection: new Estoque()}));
		});
	});
});