"use strict";

COI.module("Cheques", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Cheque = Backbone.Model.extend({
		defaults: {
			valor: null,
			data: null,
			paciente: null
		}
	});

	var Cheques = Backbone.Collection.extend({
		url: 'rest/cheques',
		model: Cheque
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