"use strict";

COI.module("Laudos", function(Module, COI, Backbone, Marionette, $, _) {
	
	var Laudo = Backbone.Model.extend({
		defaults: {
			data: null,
			status: null,
			paciente: null
		}
	});

	var Laudos = Backbone.Collection.extend({
		url: 'rest/laudos',
		model: Laudo
	});
	
	var RowView = COI.ActionRowView.extend({
		template: '#laudo_row_template',
		onRender: function() {
			var that = this;
			this.$('.coi-action-print').click(function(e) {
				if (e && e.preventDefault) e.preventDefault();
				if (e && e.stopPropagation) e.stopPropagation();
				var args = {
					view : that,
					model : that.model,
					collection : that.collection
				};
				that.triggerMethod('print', args);
			}).button();
		},
		onUpdate: function(e) {
			Backbone.history.navigate('laudo/' + this.model.get('id'), true);
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
		},
		onPrint: function(e) {
            this.$el.append($('<iframe/>', {'src': 'rest/laudos/print/' + this.model.get('id')}).hide());
		}
	});
	
	var LaudosView = COI.GridView.extend({
		itemView: RowView,
		templateHelpers: {
			header: 'Laudos',
			columns: {
				paciente: 'Paciente',
				data: 'Data',
				status: 'Status'
			}
		},
		initialize: function() {
			this.collection.fetch();
		},
		onCancel: function(e) {
			Backbone.history.navigate('', true);
		},
		onCreate: function(e) {
			Backbone.history.navigate('laudo', true);
		}
	});
	
	Module.on('start', function(options) {
		COI.router.route('laudos', function() {
			COI.body.show(new LaudosView({collection: new Laudos()}));
		});
	});
});