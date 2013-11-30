"use strict";

COI.module("Pagamentos", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Pagamento = Backbone.Model.extend({
		defaults: {
			vencimento: null,
			descricao: null,
			valor: null
		},
		parse: function(resp, options) {
			resp.vencimento = $.datepicker.formatDate('dd/mm/yy', $.datepicker.parseDate('yy-mm-dd', resp.vencimento));
			return resp;
		}
	});

	var Pagamentos = Backbone.Collection.extend({
		url: '/rest/pagamentos',
		model: Pagamento
	});
	
	var RowView = COI.ActionRowView.extend({
		template: '#pagamento_row_template',
		onUpdate: function(e) {
			Backbone.history.navigate('pagamento/' + this.model.get('id'), true);
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
			header: 'Pagamentos',
			columns: {
				data: 'Vencimento',
				descricao: 'Descrição',
				valor: 'Valor'
			}
		},
		extras: {
			'pagar': {
				text: 'À Pagar',
				trigger: 'pagar'
			},
			'pago': {
				text: 'Pagos',
				trigger: 'pagos'
			}
		},
		initialize: function() {
			this.collection.fetch();
		},
		onCancel: function(e) {
			Backbone.history.navigate('', true);
		},
		onCreate: function(e) {
			Backbone.history.navigate('pagamento', true);
		},
		onPagar: function(e) {
			this.$el.append($('<iframe/>', {'src': '/rest/pagamentos/pagar'}).hide());
		},
		onAnalitico: function(e) {
			this.$el.append($('<iframe/>', {'src': '/rest/pagamentos/pagos'}).hide());
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('pagamentos', function() {
			COI.body.show(new View({collection: new Pagamentos()}));
		});
	});
});