"use strict";

COI.module("Cheques", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Cheque = Backbone.Model.extend({
		defaults: {
			valor: null,
			data: null,
			paciente: null
		}
	});

	var Cheques = Backbone.Paginator.requestPager.extend({
		url: 'rest/cheques',
		model: Cheque,
		paginator_core: {
			type: 'GET',
			dataType: 'json',
			url: 'rest/cheques'
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
		template: '#cheque_row_template',
		onUpdate: function(e) {
			Backbone.history.navigate('cheque/' + this.model.get('id'), true);
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
	
	var ChequesView = COI.GridView.extend({
		searchView: COI.SimplePageView,
		itemView: RowView,
		templateHelpers: {
			header: 'Cheques',
			columns: {
				paciente: 'Beneficiário',
				data: 'Data',
				valor: 'Valor'
			}
		},
		initialize: function() {
			this.collection.fetch();
		},
		onCancel: function(e) {
			Backbone.history.navigate('', true);
		},
		onCreate: function(e) {
			Backbone.history.navigate('cheque', true);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('cheques', function() {
			COI.body.show(new ChequesView({collection: new Cheques()}));
		});
	});
});