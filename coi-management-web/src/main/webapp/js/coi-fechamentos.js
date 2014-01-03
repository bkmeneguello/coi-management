"use strict";

COI.module("Fechamento", function(Module, COI, Backbone, Marionette, $, _) {

	var Fechamento = Backbone.Model.extend({
		urlRoot: '/rest/fechamentos',
		defaults: {
			data: null,
			total: 0
		}
	});
	
	var Fechamentos = Backbone.Collection.extend({
		url: '/rest/fechamentos',
		model: Fechamento
	});
	
	var RowView = COI.ActionRowView.extend({
		template: '#fechamento_row_template',
		onUpdate: function(e) {
			Backbone.history.navigate('fechamento/' + this.model.get('id'), true);
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
	
	var View = COI.GridView.extend({
		itemView: RowView,
		templateHelpers: {
			header: 'Fechamentos',
			columns: {
				descricao: 'Data/Hora',
				valorTotal: 'Valor'
			}
		},
		extras: {
			'impressao': {
				text: 'Impressão',
				trigger: 'impressao'
			}
		},
		initialize: function() {
			this.collection.fetch();
		},
		onCancel: function(e) {
			Backbone.history.navigate('entradas', true);
		},
		onCreate: function(e) {
			Backbone.history.navigate('fechamento', true);
		},
		onImpressao: function(e) {
			this.$el.append($('<iframe/>', {'src': '/rest/fechamentos/imprimir'}).hide());
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('fechamentos', function() {
			COI.body.show(new View({collection: new Fechamentos()}));
		});
	});
	
});